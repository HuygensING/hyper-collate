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
import java.util.stream.Stream
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamException
import javax.xml.stream.events.*

const val INSTANT_DEL_XPATH_NODE = "del[@instant='true']"
const val SEQ0_DEL_XPATH_NODE = "del[@seq='0']"
const val TYPE_IMMEDIATE_DEL_XPATH_NODE = "del[@type='immediate']"

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

    fun importXML(siglum: String, xmlString: String): VariantWitnessGraph {
        val inputStream: InputStream
        return try {
            inputStream = ByteArrayInputStream(xmlString.toByteArray(charset(StandardCharsets.UTF_8.name())))
            importXML(siglum, inputStream)
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }
    }

    fun importXML(siglum: String, xmlFile: File): VariantWitnessGraph =
        try {
            val input: InputStream = FileUtils.openInputStream(xmlFile)
            importXML(siglum, input)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    fun importXML(rawSiglum: String, input: InputStream): VariantWitnessGraph {
        val siglum = rawSiglum.normalizedSiglum()
        val graph = VariantWitnessGraph(siglum)
        val witness = SimpleWitness(siglum)
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
                    else -> error("Unknown eventType: ${event.eventType}")
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
        val markup = Markup(tagName).setDepth(context.openMarkup.size)
        startElement
            .attributes
            .forEachRemaining { attr: Any ->
                val attribute = attr as Attribute
                val attributeName = attribute.name.toString()
                val attributeValue = attribute.value
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
        private val witness: SimpleWitness
    ) {
        val openMarkup: Deque<Markup> = LinkedList()
        private var lastTokenVertex: TokenVertex
        private var tokenCounter = 0L
        private val variationStartVertices: Deque<TokenVertex> =
            LinkedList() // the tokenvertices whose outgoing vertices are the variant vertices
        private val variationEndVertices: Deque<TokenVertex> =
            LinkedList() // the tokenvertices that are the last in a <del>
        private val unconnectedVertices: Deque<TokenVertex> =
            LinkedList() // the last tokenvertex in an <add> which hasn't been linked to the
        private var rdg: String? = ""
        private var parentXPath: String = ""
        private var afterDel = false
        private var afterImmediateDel = false
        private val branchCounter = AtomicInteger(0)
        private val branchIds: Deque<Int> = LinkedList()

        private val inAppStack: Deque<Boolean> = LinkedList()
        private val ignoreRdgStack: Deque<Boolean> = LinkedList()
        private val afterAppStack: Deque<Boolean> = LinkedList()
        private val unconnectedRdgVerticesStack: Deque<MutableList<TokenVertex>> = LinkedList()
        private val rdgCounter = AtomicInteger(1)

        private val inSubstStack: Deque<Boolean> = LinkedList()
        private val afterSubstStack: Deque<Boolean> = LinkedList()
        private val unconnectedSubstVerticesStack: Deque<MutableList<TokenVertex>> = LinkedList()

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
                notInAppOrSubst() && markup.isVariationStartingMarkup() -> { // del
                    variationStartVertices.push(lastTokenVertex)
                    branchIds.push(nextBranchId())
                }
                notInAppOrSubst() && markup.isVariationEndingMarkup() -> { // add
                    if (afterDel) {
                        lastTokenVertex = variationStartVertices.pop()
                    } else { // add without immediately preceding del
                        unconnectedVertices.push(lastTokenVertex) // add link from vertex preceding the <add> to vertex following </add>
                    }
                    afterDel = false
                    afterImmediateDel = false
                    branchIds.push(nextBranchId())
                }
                markup.isSubst() -> { // subst
                    variationStartVertices.push(lastTokenVertex)
                    inSubstStack.push(true)
                    unconnectedSubstVerticesStack.push(ArrayList())
                }
                markup.isApp() -> { // app
                    variationStartVertices.push(lastTokenVertex)
                    inAppStack.push(true)
                    unconnectedRdgVerticesStack.push(ArrayList())
                }
                markup.isRdg() -> { // rdg
                    rdg = markup.attributeMap["varSeq"] ?: rdgCounter.getAndIncrement().toString()
                    if (markup.isLitRdg()) {
                        ignoreRdgStack.push(true)
                    } else {
                        lastTokenVertex = variationStartVertices.peek()
                        branchIds.push(nextBranchId())
                    }
                }
                inSubst() -> {
                    lastTokenVertex = variationStartVertices.peek()
                    branchIds.push(nextBranchId())
                }
            }
        }

        fun closeMarkup(markup: Markup) {
            if (ignoreRdgStack.peek()) {
                if (markup.isRdg() && openMarkup.pop().isLitRdg()) {
                    ignoreRdgStack.pop()
                }
                return
            }
            val firstToClose = openMarkup.peek()
            if (graph.tokenVertexListForMarkup(firstToClose).isEmpty()) {
                // add milestone
                addNewToken("")
            }
            openMarkup.pop()
            parentXPath = buildParentXPath()
            val closingTag = markup.tagName
            val expectedTag = firstToClose.tagName
            if (expectedTag != closingTag) {
                throw RuntimeException("XML error: expected </$expectedTag>, got </$closingTag>")
            }
            when {
                notInAppOrSubst() && firstToClose.isVariationStartingMarkup() -> {
                    unconnectedVertices.push(lastTokenVertex)
                    branchIds.pop()
                    afterDel = true
                    afterImmediateDel = firstToClose.isImmediateDeletion()
                }
                notInAppOrSubst() && markup.isVariationEndingMarkup() -> {
                    variationEndVertices.push(lastTokenVertex)
                    branchIds.pop()
                }
                markup.isSubst() -> {
                    variationStartVertices.pop()
                    inSubstStack.pop()
                    afterSubstStack.push(true)
                }
                markup.isApp() -> {
                    variationStartVertices.pop()
                    inAppStack.pop()
                    afterAppStack.push(true)
                }
                markup.isRdg() -> {
                    unconnectedRdgVerticesStack.peek().add(lastTokenVertex)
                    branchIds.pop()
                }
                inSubst() -> {
                    afterImmediateDel =
                        markup.isVariationStartingMarkup() && firstToClose.isImmediateDeletion()
                    if (!afterImmediateDel) {
                        unconnectedSubstVerticesStack.peek().add(lastTokenVertex)
                    }
                    branchIds.pop()
                }
            }
        }

        private fun notInAppOrSubst() = notInApp() && notInSubst()

        private fun notInSubst() = !inSubst()

        private fun inSubst() = inSubstStack.peek()

        private fun notInApp() = !inAppStack.peek()

        private fun Markup.isLitRdg(): Boolean =
            "lit" == getAttributeValue("type").orElse("")

        fun addNewToken(content: String) {
            if (ignoreRdgStack.peek()) {
                return
            }
            val token = MarkedUpToken()
                .withContent(content)
                .withWitness(witness)
                .withRdg(rdg!!)
                .withIndexNumber(tokenCounter++)
                .withParentXPath(parentXPath)
                .withNormalizedContent(normalizer.apply(content))
            val tokenVertex = SimpleTokenVertex(token)
            tokenVertex.branchPath = branchIds.descendingIterator().asSequence().toList()
            graph.addOutgoingTokenVertexToTokenVertex(lastTokenVertex, tokenVertex)
            if (afterImmediateDel) {
                afterImmediateDel = false
                afterDel = false
            }
            if (afterDel) { // del without add
                // add link from vertex preceding the <del> to vertex following </del>
                graph.addOutgoingTokenVertexToTokenVertex(variationStartVertices.pop(), tokenVertex)
                unconnectedVertices.pop()
                afterDel = false
                afterImmediateDel = false
            }
            while (afterAppStack.peek()) {
                unconnectedRdgVerticesStack.pop()
                    .filter { v: TokenVertex -> v != lastTokenVertex }
                    .forEach { v: TokenVertex -> graph.addOutgoingTokenVertexToTokenVertex(v, tokenVertex) }
                afterAppStack.pop()
            }
            while (afterSubstStack.peek()) {
                unconnectedSubstVerticesStack.pop()
                    .filter { v: TokenVertex -> v != lastTokenVertex }
                    .forEach { v: TokenVertex -> graph.addOutgoingTokenVertexToTokenVertex(v, tokenVertex) }
                afterSubstStack.pop()
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
            (unconnectedSubstVerticesStack + unconnectedRdgVerticesStack)
                .flatten()
                .filter { v: TokenVertex -> v != lastTokenVertex }
                .forEach { v: TokenVertex -> graph.addOutgoingTokenVertexToTokenVertex(v, endTokenVertex) }
            unconnectedSubstVerticesStack.clear()
            unconnectedRdgVerticesStack.clear()
        }

        private fun buildParentXPath(): String =
            "/" + openMarkup.reversed().joinToString("/") { it.xpathNode() }

        private fun Markup.xpathNode(): String =
            when (this.tagName) {
                "rdg" -> {
                    val identifyingAttribute = this.attributeMap.keys.filter { it in rdgIdentifyingAttributes }
                    if (identifyingAttribute.isEmpty()) {
                        "rdg"
                    } else {
                        val attr = identifyingAttribute[0]
                        val value = this.attributeMap[attr]
                        "rdg[@$attr='$value']"
                    }
                }
                "del" -> {
                    when {
                        this.attributeMap["instant"] == "true" -> {
                            INSTANT_DEL_XPATH_NODE
                        }
                        this.attributeMap["seq"] == "0" -> {
                            SEQ0_DEL_XPATH_NODE
                        }
                        this.attributeMap["type"] == "immediate" -> {
                            TYPE_IMMEDIATE_DEL_XPATH_NODE
                        }
                        else -> this.tagName
                    }
                }
                else -> this.tagName
            }

        init {
            lastTokenVertex = graph.startTokenVertex

            afterSubstStack.push(false)
            inSubstStack.push(false)
            unconnectedSubstVerticesStack.push(ArrayList())

            afterAppStack.push(false)
            ignoreRdgStack.push(false)
            inAppStack.push(false)
            unconnectedRdgVerticesStack.push(ArrayList())

            branchIds.push(nextBranchId())
        }

        companion object {
            val rdgIdentifyingAttributes = setOf("wit", "varSeq", "type")
        }
    }

    companion object {
        fun String.normalizedSiglum(): String = replace("[^0-9a-zA-Z]".toRegex(), "")

        private fun Markup.isSubst(): Boolean = "subst" == tagName
        private fun Markup.isApp(): Boolean = "app" == tagName
        private fun Markup.isRdg(): Boolean = "rdg" == tagName

        private fun Markup.isVariationStartingMarkup(): Boolean =
            "del" == tagName && !this.isImmediateDeletion()

        private fun Markup.isVariationEndingMarkup(): Boolean = "add" == tagName

        private fun Markup.isImmediateDeletion() =
            "true" == attributeMap["instant"]
                    || "0" == attributeMap["seq"]
                    || "immediate" == attributeMap["type"]

    }
}
