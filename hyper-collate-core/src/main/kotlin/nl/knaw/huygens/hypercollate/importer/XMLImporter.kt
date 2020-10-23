package nl.knaw.huygens.hypercollate.importer

/*-
 * #%L
 * hyper-collate-core
 * =======
 * Copyright (C) 2017 - 2020 Huygens ING (KNAW)
 * =======
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import eu.interedition.collatex.simple.SimplePatternTokenizer
import nl.knaw.huygens.hypercollate.model.*
import org.apache.commons.io.FileUtils
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.Stream
import java.util.stream.StreamSupport
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamException
import javax.xml.stream.events.*

class XMLImporter {
    private val tokenizer: Function<String, Stream<String>>
    private val normalizer: Function<String, String>

    constructor(
            tokenizer: Function<String, Stream<String>>,
            normalizer: Function<String, String>
    ) {
        this.tokenizer = tokenizer
        this.normalizer = normalizer
    }

    constructor() {
        tokenizer = SimplePatternTokenizer.BY_WS_OR_PUNCT
        normalizer = Function { raw: String -> raw.trim { it <= ' ' }.toLowerCase() }
    }

    fun importXML(sigil: String, xmlString: String): VariantWitnessGraph {
        val inputStream: InputStream
        return try {
            inputStream = ByteArrayInputStream(xmlString.toByteArray(charset(StandardCharsets.UTF_8.name())))
            importXML(sigil, inputStream)
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }
    }

    fun importXML(sigil: String, xmlFile: File): VariantWitnessGraph =
            try {
                val input: InputStream = FileUtils.openInputStream(xmlFile)
                importXML(sigil, input)
            } catch (e: IOException) {
                throw RuntimeException(e)
            }

    fun importXML(rawSigil: String, input: InputStream): VariantWitnessGraph {
        val sigil = normalizedSigil(rawSigil)
        val graph = VariantWitnessGraph(sigil)
        val witness = SimpleWitness(sigil)
        val factory = XMLInputFactory.newInstance()
        return try {
            val reader = factory.createXMLEventReader(input)
            val context = Context(graph, normalizer, witness)
            while (reader.hasNext()) {
                val event = reader.nextEvent()
                when (event.eventType) {
                    XMLStreamConstants.START_DOCUMENT -> handleStartDocument(event, context)
                    XMLStreamConstants.START_ELEMENT -> handleStartElement(event.asStartElement(), context)
                    XMLStreamConstants.CHARACTERS -> handleCharacters(event.asCharacters(), context)
                    XMLStreamConstants.END_ELEMENT -> handleEndElement(event.asEndElement(), context)
                    XMLStreamConstants.END_DOCUMENT -> handleEndDocument(event, context)
                    XMLStreamConstants.PROCESSING_INSTRUCTION -> handleProcessingInstruction(event, context)
                    XMLStreamConstants.COMMENT -> handleComment(event, context)
                    XMLStreamConstants.SPACE -> handleSpace(event, context)
                    XMLStreamConstants.ENTITY_REFERENCE -> handleEntityReference(event, context)
                    XMLStreamConstants.ATTRIBUTE -> handleAttribute(event, context)
                    XMLStreamConstants.DTD -> handleDTD(event, context)
                    XMLStreamConstants.CDATA -> handleCData(event, context)
                    XMLStreamConstants.NAMESPACE -> handleNameSpace(event, context)
                    XMLStreamConstants.NOTATION_DECLARATION -> handleNotationDeclaration(event, context)
                    XMLStreamConstants.ENTITY_DECLARATION -> handleEntityDeclaration(event, context)
                    else -> {
                    }
                }
            }
            graph
        } catch (e: XMLStreamException) {
            throw RuntimeException(e)
        }
    }

    private fun handleStartDocument(event: XMLEvent, context: Context) {}

    private fun handleEndDocument(event: XMLEvent, context: Context) = context.closeDocument()

    private fun handleStartElement(startElement: StartElement, context: Context) {
        val tagName = startElement.name.toString()
        val markup = Markup(tagName).withDepth(context.openMarkup.size)
        startElement
                .attributes
                .forEachRemaining { `object`: Any ->
                    val attribute = `object` as Attribute
                    val attributeName = attribute.name.toString()
                    val attributeValue = `object`.value
                    markup.addAttribute(attributeName, attributeValue)
                }
        context.openMarkup(markup)
    }

    private fun handleEndElement(endElement: EndElement, context: Context) {
        val tagName = endElement.name.toString()
        val markup = Markup(tagName)
        context.closeMarkup(markup)
    }

    private fun handleCharacters(characters: Characters, context: Context) {
        val data = characters.data
        if (data.startsWith(" ")) { // because the tokenizer will lose these leading whitespaces;
            context.addNewToken(" ")
        }
        tokenizer.apply(data).forEach { content: String -> context.addNewToken(content) }
    }

    private fun handleNotationDeclaration(event: XMLEvent, context: Context): Unit =
            throw RuntimeException("unexpected event: NotationDeclaration")

    private fun handleEntityDeclaration(event: XMLEvent, context: Context): Unit =
            throw RuntimeException("unexpected event: EntityDeclaration")

    private fun handleNameSpace(event: XMLEvent, context: Context): Unit =
            throw RuntimeException("unexpected event: NameSpace")

    private fun handleCData(event: XMLEvent, context: Context): Unit = throw RuntimeException("unexpected event: CData")

    private fun handleDTD(event: XMLEvent, context: Context): Unit = throw RuntimeException("unexpected event: DTD")

    private fun handleAttribute(event: XMLEvent, context: Context): Unit =
            throw RuntimeException("unexpected event: Attribute")

    private fun handleEntityReference(event: XMLEvent, context: Context): Unit =
            throw RuntimeException("unexpected event: EntityReference")

    private fun handleSpace(event: XMLEvent, context: Context): Unit = throw RuntimeException("unexpected event: Space")

    private fun handleComment(event: XMLEvent, context: Context) {}

    private fun handleProcessingInstruction(event: XMLEvent, context: Context): Unit =
            throw RuntimeException("unexpected event: ProcessingInstruction")

    private class Context(
            private val graph: VariantWitnessGraph, // tokenvertex after the </del> yet
            private val normalizer: Function<String, String>,
            witness: SimpleWitness
    ) {
        val openMarkup: Deque<Markup> = LinkedList()
        private var lastTokenVertex: TokenVertex
        private var tokenCounter = 0L
        private val variationStartVertices: Deque<TokenVertex> = LinkedList() // the tokenvertices whose outgoing vertices are the variant vertices

        // (add/del)
        private val variationEndVertices: Deque<TokenVertex> = LinkedList() // the tokenvertices that are the last in a <del>
        private val unconnectedVertices: Deque<TokenVertex> = LinkedList() // the last tokenvertex in an <add> which hasn't been linked to the
        private val witness: SimpleWitness
        private var rdg: String? = ""
        private var parentXPath: String? = null
        private var afterDel = false
        private val branchCounter = AtomicInteger(0)
        private val branchIds: Deque<Int> = LinkedList()
        private val inAppStack: Deque<Boolean> = LinkedList()
        private val ignoreRdgStack: Deque<Boolean> = LinkedList()
        private val afterAppStack: Deque<Boolean> = LinkedList()
        private val unconnectedRdgVerticesStack: Deque<MutableList<TokenVertex>> = LinkedList()
        private val rdgCounter = AtomicInteger(1)

        private fun nextBranchId(): Int = branchCounter.getAndIncrement()

        fun openMarkup(markup: Markup) {
            if (markup.tagName == "app") {
                rdgCounter.set(1)
            }
            if (ignoreRdgStack.peek()) {
                return
            }
            graph.addMarkup(markup)
            openMarkup.push(markup)
            parentXPath = buildParentXPath()
            when {
                !inAppStack.peek() && isVariationStartingMarkup(markup) -> { // del
                    variationStartVertices.push(lastTokenVertex)
                    branchIds.push(nextBranchId())
                }
                !inAppStack.peek() && isVariationEndingMarkup(markup) -> { // add
                    if (afterDel) {
                        lastTokenVertex = variationStartVertices.pop()
                    } else { // add without immediately preceding del
                        unconnectedVertices.push(
                                lastTokenVertex) // add link from vertex preceding the <add> to vertex following
                        // </add>
                    }
                    afterDel = false
                    branchIds.push(nextBranchId())
                }
                isApp(markup) -> { // app
                    variationStartVertices.push(lastTokenVertex)
                    inAppStack.push(true)
                    unconnectedRdgVerticesStack.push(ArrayList())
                }
                isRdg(markup) -> { // rdg
                    rdg = markup.attributeMap["varSeq"] ?: rdgCounter.getAndIncrement().toString()
                    if (isLitRdg(markup)) {
                        ignoreRdgStack.push(true)
                    } else {
                        lastTokenVertex = variationStartVertices.peek()
                        branchIds.push(nextBranchId())
                    }
                }
            }
        }

        private fun isApp(markup: Markup): Boolean =
                "app" == markup.tagName

        private fun isRdg(markup: Markup): Boolean =
                "rdg" == markup.tagName

        private fun isVariationStartingMarkup(markup: Markup): Boolean =
                "del" == markup.tagName

        private fun isVariationEndingMarkup(markup: Markup): Boolean =
                "add" == markup.tagName

        fun closeMarkup(markup: Markup) {
            if (ignoreRdgStack.peek()) {
                if (isRdg(markup) && isLitRdg(openMarkup.pop())) {
                    ignoreRdgStack.pop()
                }
                return
            }
            val firstToClose = openMarkup.peek()
            if (graph.getTokenVertexListForMarkup(firstToClose).isEmpty()) {
                // add milestone
                addNewToken("")
            }
            openMarkup.pop()
            parentXPath = buildParentXPath()
            val closingTag = markup.tagName
            val expectedTag = firstToClose.tagName
            if (expectedTag != closingTag) {
                throw RuntimeException(
                        "XML error: expected </$expectedTag>, got </$closingTag>")
            }
            when {
                !inAppStack.peek() && isVariationStartingMarkup(markup) -> {
                    unconnectedVertices.push(lastTokenVertex)
                    branchIds.pop()
                    afterDel = true
                }
                !inAppStack.peek() && isVariationEndingMarkup(markup) -> {
                    variationEndVertices.push(lastTokenVertex)
                    branchIds.pop()
                }
                isApp(markup) -> {
                    variationStartVertices.pop()
                    inAppStack.pop()
                    afterAppStack.push(true)
                }
                isRdg(markup) -> {
                    unconnectedRdgVerticesStack.peek().add(lastTokenVertex)
                    branchIds.pop()
                }
            }
        }

        private fun isLitRdg(markup: Markup): Boolean =
                "lit" == markup.getAttributeValue("type").orElse("")

        fun addNewToken(content: String) {
            if (ignoreRdgStack.peek()) {
                return
            }
            val token = MarkedUpToken()
                    .withContent(content)
                    .withWitness(witness)
                    .withRdg(rdg!!)
                    .withIndexNumber(tokenCounter++)
                    .withParentXPath(parentXPath!!)
                    .withNormalizedContent(normalizer.apply(content))
            val tokenVertex = SimpleTokenVertex(token)
            tokenVertex.branchPath = branchIds.descendingIterator().asSequence().toList()
            graph.addOutgoingTokenVertexToTokenVertex(lastTokenVertex, tokenVertex)
            if (afterDel) { // del without add
                // add link from vertex preceding the <del> to vertex following </del>
                graph.addOutgoingTokenVertexToTokenVertex(variationStartVertices.pop(), tokenVertex)
                unconnectedVertices.pop()
                afterDel = false
            }
            while (afterAppStack.peek()) {
                unconnectedRdgVerticesStack.pop()
                        .filter { v: TokenVertex -> v != lastTokenVertex }
                        .forEach { v: TokenVertex -> graph.addOutgoingTokenVertexToTokenVertex(v, tokenVertex) }
                afterAppStack.pop()
            }
            openMarkup
                    .descendingIterator()
                    .forEachRemaining { markup: Markup -> graph.addMarkupToTokenVertex(tokenVertex, markup) }
            checkUnconnectedVertices(tokenVertex)
            lastTokenVertex = tokenVertex
        }

        private fun checkUnconnectedVertices(tokenVertex: SimpleTokenVertex) {
            if (!variationEndVertices.isEmpty() && lastTokenVertex == variationEndVertices.peek()) {
                variationEndVertices.pop()
                if (!unconnectedVertices.isEmpty()) {
                    val unconnectedVertex = unconnectedVertices.pop()
                    graph.addOutgoingTokenVertexToTokenVertex(unconnectedVertex, tokenVertex)
                    checkUnconnectedVertices(tokenVertex)
                }
            }
        }

        fun closeDocument() {
            val endTokenVertex = graph.endTokenVertex
            graph.addOutgoingTokenVertexToTokenVertex(lastTokenVertex, endTokenVertex)
            while (!unconnectedVertices.isEmpty()) {
                // add link from vertex preceding the <add> to end vertex
                val unconnectedVertex = unconnectedVertices.pop()
                if (unconnectedVertex != lastTokenVertex) {
                    graph.addOutgoingTokenVertexToTokenVertex(unconnectedVertex, endTokenVertex)
                }
            }
            while (!variationStartVertices.isEmpty()) {
                // add link from vertex preceding the <del> to end vertex
                graph.addOutgoingTokenVertexToTokenVertex(variationStartVertices.pop(), endTokenVertex)
            }
        }

        private fun buildParentXPath(): String =
                ("/"
                        + streamIterator(openMarkup.descendingIterator())
                        .map { obj: Markup -> obj.tagName }
                        .collect(Collectors.joining("/")))

        private fun streamIterator(iterator: Iterator<Markup>): Stream<Markup> {
            val spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED)
            return StreamSupport.stream(spliterator, false)
        }

        init {
            lastTokenVertex = graph.startTokenVertex
            this.witness = witness
            afterAppStack.push(false)
            ignoreRdgStack.push(false)
            inAppStack.push(false)
            unconnectedRdgVerticesStack.push(ArrayList())
            branchIds.push(nextBranchId())
        }
    }

    companion object {
        fun normalizedSigil(rawSigil: String): String {
            return rawSigil.replace("[^0-9a-zA-Z]".toRegex(), "")
        }
    }
}
