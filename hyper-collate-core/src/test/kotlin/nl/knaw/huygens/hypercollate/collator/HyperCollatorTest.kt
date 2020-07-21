package nl.knaw.huygens.hypercollate.collator

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

import com.google.common.base.Stopwatch
import eu.interedition.collatex.dekker.Tuple
import nl.knaw.huygens.hypercollate.HyperCollateAssertions.assertThat
import nl.knaw.huygens.hypercollate.HyperCollateTest
import nl.knaw.huygens.hypercollate.importer.XMLImporter
import nl.knaw.huygens.hypercollate.model.*
import nl.knaw.huygens.hypercollate.tools.CollationGraphNodeJoiner
import nl.knaw.huygens.hypercollate.tools.CollationGraphVisualizer
import org.assertj.core.util.Sets
import org.junit.Test
import org.slf4j.LoggerFactory
import java.text.MessageFormat.format
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

class HyperCollatorTest : HyperCollateTest() {
    private val hyperCollator = HyperCollator()

    //    @Ignore("takes too long")
    @Test(timeout = 40_000)
    fun testCollationWithManyMatches() {
        val importer = XMLImporter()
        val xml1 = ("<seg>Ik had een buurvrouw, een paar deuren verder,"
                + " en <del>ze</del><add>het</add> was zo'n type dat naar het muse<del>im</del>um ging en "
                + "cappuc<add>c</add>i<del>o</del>no's dronk<del>l</del>, dus ik<del>i k</del>kon er weinig mee, en zij kon weinig"
                + " m<del>netr</del>et mij<del>,</del><add>;</add> we <del>lk</del> knikten alleen naar elkaar, en als ik"
                + " Rock<del>u</del>y bij me had, <del>knikte</del>maakte ze van het knikken iets dat nog wat sneller "
                + "a<del >g</del>fgehandeld moest<del>r</del> worden dan anders.</seg>")
        val w1 = importer.importXML("W1", xml1)
        val xml2 = ("<seg><del>Ik had een buurvrouw, </del><add>Die "
                + "buurvrouw woonde </add>een paar deuren verder, en het was zo'n type <del>dat naar het museum ging en "
                + "cappuccino's dronk, dus ik kon er</del><add>waar ik</add> weinig mee<add> ko<del>m</del>n</add>, en zij kon "
                + "weinig met mij; we knikten alleen naar elkaar, en als ik Rocky bij me had, maakte ze van het knikken iets dat"
                + " nog wat sneller afgehandeld moest worden dan anders.</seg>")
        val w2 = importer.importXML("W2", xml2)
        val expectedDot = """
            digraph CollationGraph{
            labelloc=b
            t000 [label="";shape=doublecircle,rank=middle]
            t001 [label="";shape=doublecircle,rank=middle]
            t002 [label=<W1,W2: Ik&#9251;had&#9251;een&#9251;buurvrouw,&#9251;<br/>W1: <i>/seg</i><br/>W2: <i>/seg/del</i><br/>>]
            t003 [label=<W1: ze<br/>W1: <i>/seg/del</i>>]
            t004 [label=<W1: het<br/>W2: het&#9251;<br/>W1: <i>/seg/add</i><br/>W2: <i>/seg</i><br/>>]
            t005 [label=<W1: &#9251;<br/>W1: <i>/seg</i>>]
            t006 [label=<W1,W2: was&#9251;zo'n&#9251;type&#9251;<br/>W1,W2: <i>/seg</i>>]
            t007 [label=<W1,W2: dat&#9251;naar&#9251;het&#9251;<br/>W1: <i>/seg</i><br/>W2: <i>/seg/del</i><br/>>]
            t008 [label=<W1: muse<br/>W1: <i>/seg</i>>]
            t009 [label=<W1: im<br/>W1: <i>/seg/del</i>>]
            t010 [label=<W1: um&#9251;<br/>W1: <i>/seg</i>>]
            t011 [label=<W1,W2: ging&#9251;en&#9251;<br/>W1: <i>/seg</i><br/>W2: <i>/seg/del</i><br/>>]
            t012 [label=<W1: cappuc<br/>W1: <i>/seg</i>>]
            t013 [label=<W1: c<br/>W1: <i>/seg/add</i>>]
            t014 [label=<W1: i<br/>W1: <i>/seg</i>>]
            t015 [label=<W1: o<br/>W1: <i>/seg/del</i>>]
            t016 [label=<W1: no's&#9251;<br/>W1: <i>/seg</i>>]
            t017 [label=<W1,W2: dronk<br/>W1: <i>/seg</i><br/>W2: <i>/seg/del</i><br/>>]
            t018 [label=<W1: l<br/>W1: <i>/seg/del</i>>]
            t019 [label=<W1: ,&#9251;dus&#9251;ik<br/>W2: ,&#9251;dus&#9251;ik&#9251;<br/>W1: <i>/seg</i><br/>W2: <i>/seg/del</i><br/>>]
            t020 [label=<W1: i&#9251;k<br/>W1: <i>/seg/del</i>>]
            t021 [label=<W1: kon&#9251;er&#9251;<br/>W2: kon&#9251;er<br/>W1: <i>/seg</i><br/>W2: <i>/seg/del</i><br/>>]
            t022 [label=<W1,W2: weinig&#9251;mee<br/>W1,W2: <i>/seg</i>>]
            t023 [label=<W1,W2: ,&#9251;en&#9251;zij&#9251;kon&#9251;weinig&#9251;<br/>W1,W2: <i>/seg</i>>]
            t024 [label=<W1: m<br/>W1: <i>/seg</i>>]
            t025 [label=<W1: netr<br/>W1: <i>/seg/del</i>>]
            t026 [label=<W1: et&#9251;<br/>W1: <i>/seg</i>>]
            t027 [label=<W1,W2: mij<br/>W1,W2: <i>/seg</i>>]
            t028 [label=<W1: ,<br/>W1: <i>/seg/del</i>>]
            t029 [label=<W1: ;<br/>W2: ;&#9251;<br/>W1: <i>/seg/add</i><br/>W2: <i>/seg</i><br/>>]
            t030 [label=<W1: &#9251;<br/>W1: <i>/seg</i>>]
            t031 [label=<W1,W2: we&#9251;<br/>W1,W2: <i>/seg</i>>]
            t032 [label=<W1: lk<br/>W1: <i>/seg/del</i>>]
            t033 [label=<W1: &#9251;<br/>W1: <i>/seg</i>>]
            t034 [label=<W1,W2: knikten&#9251;alleen&#9251;naar&#9251;elkaar,&#9251;en&#9251;als&#9251;ik&#9251;<br/>W1,W2: <i>/seg</i>>]
            t035 [label=<W1,W2: een&#9251;paar&#9251;deuren&#9251;verder,&#9251;en&#9251;<br/>W1,W2: <i>/seg</i>>]
            t036 [label=<W1: Rock<br/>W1: <i>/seg</i>>]
            t037 [label=<W1: u<br/>W1: <i>/seg/del</i>>]
            t038 [label=<W1: y&#9251;<br/>W1: <i>/seg</i>>]
            t039 [label=<W1,W2: bij&#9251;me&#9251;had,&#9251;<br/>W1,W2: <i>/seg</i>>]
            t040 [label=<W1: knikte<br/>W1: <i>/seg/del</i>>]
            t041 [label=<W1,W2: maakte&#9251;ze&#9251;van&#9251;het&#9251;knikken&#9251;iets&#9251;dat&#9251;nog&#9251;wat&#9251;sneller&#9251;<br/>W1,W2: <i>/seg</i>>]
            t042 [label=<W1: a<br/>W1: <i>/seg</i>>]
            t043 [label=<W1: g<br/>W1: <i>/seg/del</i>>]
            t044 [label=<W1: fgehandeld&#9251;<br/>W1: <i>/seg</i>>]
            t045 [label=<W1: moest<br/>W2: moest&#9251;<br/>W1,W2: <i>/seg</i>>]
            t046 [label=<W1: r<br/>W1: <i>/seg/del</i>>]
            t047 [label=<W1: &#9251;<br/>W1: <i>/seg</i>>]
            t048 [label=<W1,W2: worden&#9251;dan&#9251;anders.<br/>W1,W2: <i>/seg</i>>]
            t049 [label=<W2: museum&#9251;<br/>W2: <i>/seg/del</i>>]
            t050 [label=<W2: cappuccino's&#9251;<br/>W2: <i>/seg/del</i>>]
            t051 [label=<W2: waar&#9251;ik<br/>W2: <i>/seg/add</i>>]
            t052 [label=<W2: &#9251;<br/>W2: <i>/seg</i>>]
            t053 [label=<W2: &#9251;ko<br/>W2: <i>/seg/add</i>>]
            t054 [label=<W2: m<br/>W2: <i>/seg/add/del</i>>]
            t055 [label=<W2: n<br/>W2: <i>/seg/add</i>>]
            t056 [label=<W2: met&#9251;<br/>W2: <i>/seg</i>>]
            t057 [label=<W2: Rocky&#9251;<br/>W2: <i>/seg</i>>]
            t058 [label=<W2: Die&#9251;buurvrouw&#9251;woonde&#9251;<br/>W2: <i>/seg/add</i>>]
            t059 [label=<W2: afgehandeld&#9251;<br/>W2: <i>/seg</i>>]
            t000->t002[label="W1,W2"]
            t000->t058[label="W2"]
            t002->t035[label="W1,W2"]
            t003->t005[label="W1"]
            t004->t005[label="W1"]
            t004->t006[label="W2"]
            t005->t006[label="W1"]
            t006->t007[label="W1,W2"]
            t006->t051[label="W2"]
            t007->t008[label="W1"]
            t007->t049[label="W2"]
            t008->t009[label="W1"]
            t008->t010[label="W1"]
            t009->t010[label="W1"]
            t010->t011[label="W1"]
            t011->t012[label="W1"]
            t011->t050[label="W2"]
            t012->t013[label="W1"]
            t012->t014[label="W1"]
            t013->t014[label="W1"]
            t014->t015[label="W1"]
            t014->t016[label="W1"]
            t015->t016[label="W1"]
            t016->t017[label="W1"]
            t017->t018[label="W1"]
            t017->t019[label="W1,W2"]
            t018->t019[label="W1"]
            t019->t020[label="W1"]
            t019->t021[label="W1,W2"]
            t020->t021[label="W1"]
            t021->t022[label="W1"]
            t021->t052[label="W2"]
            t022->t023[label="W1,W2"]
            t022->t053[label="W2"]
            t023->t024[label="W1"]
            t023->t056[label="W2"]
            t024->t025[label="W1"]
            t024->t026[label="W1"]
            t025->t026[label="W1"]
            t026->t027[label="W1"]
            t027->t028[label="W1"]
            t027->t029[label="W1,W2"]
            t028->t030[label="W1"]
            t029->t030[label="W1"]
            t029->t031[label="W2"]
            t030->t031[label="W1"]
            t031->t032[label="W1"]
            t031->t033[label="W1"]
            t031->t034[label="W2"]
            t032->t033[label="W1"]
            t033->t034[label="W1"]
            t034->t036[label="W1"]
            t034->t057[label="W2"]
            t035->t003[label="W1"]
            t035->t004[label="W1,W2"]
            t036->t037[label="W1"]
            t036->t038[label="W1"]
            t037->t038[label="W1"]
            t038->t039[label="W1"]
            t039->t040[label="W1"]
            t039->t041[label="W1,W2"]
            t040->t041[label="W1"]
            t041->t042[label="W1"]
            t041->t059[label="W2"]
            t042->t043[label="W1"]
            t042->t044[label="W1"]
            t043->t044[label="W1"]
            t044->t045[label="W1"]
            t045->t046[label="W1"]
            t045->t047[label="W1"]
            t045->t048[label="W2"]
            t046->t047[label="W1"]
            t047->t048[label="W1"]
            t048->t001[label="W1,W2"]
            t049->t011[label="W2"]
            t050->t017[label="W2"]
            t051->t052[label="W2"]
            t052->t022[label="W2"]
            t053->t054[label="W2"]
            t053->t055[label="W2"]
            t054->t055[label="W2"]
            t055->t023[label="W2"]
            t056->t027[label="W2"]
            t057->t039[label="W2"]
            t058->t035[label="W2"]
            t059->t045[label="W2"]
            }""".trimIndent()
        val expectedTable = """
            ┌────┬──────────────────────────┬───────────────────────────┬───────┬─┬──────────────┬─────────────────┬───────────┬──────┬───┬────────────┬─────────────────┬─────┬─┬─────┬─────┬─────────┬─────┬─────────────┬───────┬──────────┬─┬──────────┬───────┬─────┬─────┬────────────────────┬────┬────────┬───┬───┬─────┬─┬───┬──────┬─┬──────────────────────────────────────┬──────┬─────┬──┬────────────┬──────────┬───────────────────────────────────────────────────┬────────────┬─────┬───────────┬──────┬─────┬─┬──────────────────┐
            │[W1]│                          │                           │[+] het│ │              │                 │           │      │   │            │                 │     │ │     │     │         │     │             │       │          │ │          │       │     │     │                    │    │        │   │   │[+] ;│ │   │      │ │                                      │      │     │  │            │          │                                                   │            │     │           │      │     │ │                  │
            │    │Ik had een buurvrouw,     │een paar deuren verder, en │[-] ze │ │was zo'n type │dat naar het     │muse       │[-] im│um │ging en     │cappuc           │[+] c│i│[-] o│no's │dronk    │[-] l│, dus ik     │[-] i k│kon er    │ │weinig mee│       │     │     │, en zij kon weinig │m   │[-] netr│et │mij│[-] ,│ │we │[-] lk│ │knikten alleen naar elkaar, en als ik │Rock  │[-] u│y │bij me had, │[-] knikte│maakte ze van het knikken iets dat nog wat sneller │a           │[-] g│fgehandeld │moest │[-] r│ │worden dan anders.│
            ├────┼──────────────────────────┼───────────────────────────┼───────┼─┼──────────────┼─────────────────┼───────────┼──────┼───┼────────────┼─────────────────┼─────┼─┼─────┼─────┼─────────┼─────┼─────────────┼───────┼──────────┼─┼──────────┼───────┼─────┼─────┼────────────────────┼────┼────────┼───┼───┼─────┼─┼───┼──────┼─┼──────────────────────────────────────┼──────┼─────┼──┼────────────┼──────────┼───────────────────────────────────────────────────┼────────────┼─────┼───────────┼──────┼─────┼─┼──────────────────┤
            │[W2]│[+] Die  buurvrouw  woonde│                           │       │ │              │[+]    waar    ik│           │      │   │            │                 │     │ │     │     │         │     │             │       │          │ │          │       │     │     │                    │    │        │   │   │     │ │   │      │ │                                      │      │     │  │            │          │                                                   │            │     │           │      │     │ │                  │
            │    │[-] Ik had een buurvrouw, │een paar deuren verder, en │het    │ │was zo'n type │[-] dat naar het │[-] museum │      │   │[-] ging en │[-] cappuccino's │     │ │     │     │[-] dronk│     │[-] , dus ik │       │[-] kon er│ │weinig mee│[+] ko │[-] m│[+] n│, en zij kon weinig │met │        │   │mij│;    │ │we │      │ │knikten alleen naar elkaar, en als ik │Rocky │     │  │bij me had, │          │maakte ze van het knikken iets dat nog wat sneller │afgehandeld │     │           │moest │     │ │worden dan anders.│
            └────┴──────────────────────────┴───────────────────────────┴───────┴─┴──────────────┴─────────────────┴───────────┴──────┴───┴────────────┴─────────────────┴─────┴─┴─────┴─────┴─────────┴─────┴─────────────┴───────┴──────────┴─┴──────────┴───────┴─────┴─────┴────────────────────┴────┴────────┴───┴───┴─────┴─┴───┴──────┴─┴──────────────────────────────────────┴──────┴─────┴──┴────────────┴──────────┴───────────────────────────────────────────────────┴────────────┴─────┴───────────┴──────┴─────┴─┴──────────────────┘
            """.trimIndent()
        LOG.info("w1={}", xml1)
        LOG.info("w2={}", xml2)
        val collationGraph = testHyperCollation(w1, w2, expectedDot, expectedTable)
    }

    @Test//(timeout = 10000)
    fun testAppRdgWithAddDel() {
        val importer = XMLImporter()
        val wF = importer.importXML(
                "W1",
                "<s>One must have lived longer with <app>"
                        + "<rdg varSeq=\"1\"><del>this</del></rdg>"
                        + "<rdg varSeq=\"2\"><del><add>such a</add></del></rdg>"
                        + "<rdg varSeq=\"3\"><add>a</add></rdg>"
                        + "</app> system, to appreciate its advantages.</s>")
        val wQ = importer.importXML(
                "W2",
                "<s>One must have lived longer with this system, to appreciate its advantages.</s>")
        val expectedDot = """
            digraph CollationGraph{
            labelloc=b
            t000 [label="";shape=doublecircle,rank=middle]
            t001 [label="";shape=doublecircle,rank=middle]
            t002 [label=<W1,W2: One&#9251;must&#9251;have&#9251;lived&#9251;longer&#9251;with&#9251;<br/>W1,W2: <i>/s</i>>]
            t003 [label=<W1: &#9251;<br/>W1: <i>/s</i>>]
            t004 [label=<W1,W2: system,&#9251;to&#9251;appreciate&#9251;its&#9251;advantages.<br/>W1,W2: <i>/s</i>>]
            t005 [label=<W1: this<br/>W2: this&#9251;<br/>W1: <i>/s/app/rdg/del</i><br/>W2: <i>/s</i><br/>>]
            t006 [label=<W1: such&#9251;a<br/>W1: <i>/s/app/rdg/del/add</i>>]
            t007 [label=<W1: a<br/>W1: <i>/s/app/rdg/add</i>>]
            t000->t002[label="W1,W2"]
            t002->t005[label="W1,W2"]
            t002->t006[label="W1"]
            t002->t007[label="W1"]
            t003->t004[label="W1"]
            t004->t001[label="W1,W2"]
            t005->t003[label="W1"]
            t005->t004[label="W2"]
            t006->t003[label="W1"]
            t007->t003[label="W1"]
            }
            """.trimIndent()
        val expectedTable = """
            ┌────┬────────────────────────────────┬──────────────┬─┬─────────────────────────────────────┐
            │[W1]│                                │<3>[+]       a│ │                                     │
            │    │                                │<2>[+-] such a│ │                                     │
            │    │One must have lived longer with │<1>[-] this   │ │system, to appreciate its advantages.│
            ├────┼────────────────────────────────┼──────────────┼─┼─────────────────────────────────────┤
            │[W2]│One must have lived longer with │this          │ │system, to appreciate its advantages.│
            └────┴────────────────────────────────┴──────────────┴─┴─────────────────────────────────────┘
            """.trimIndent()
        val collationGraph = testHyperCollation(wF, wQ, expectedDot, expectedTable)
    }

    @Test(timeout = 10000)
    fun testAppRdg() {
        val importer = XMLImporter()
        val wF = importer.importXML(
                "W1",
                "<s>One must have lived longer with <app>"
                        + "<rdg>this</rdg>"
                        + "<rdg>such a</rdg>"
                        + "<rdg>a</rdg>"
                        + "</app> system, to appreciate its advantages.</s>")
        val wQ = importer.importXML(
                "W2",
                "<s>One must have lived longer with this system, to appreciate its advantages.</s>")
        val expectedDot = """
            digraph CollationGraph{
            labelloc=b
            t000 [label="";shape=doublecircle,rank=middle]
            t001 [label="";shape=doublecircle,rank=middle]
            t002 [label=<W1,W2: One&#9251;must&#9251;have&#9251;lived&#9251;longer&#9251;with&#9251;<br/>W1,W2: <i>/s</i>>]
            t003 [label=<W1: &#9251;<br/>W1: <i>/s</i>>]
            t004 [label=<W1,W2: system,&#9251;to&#9251;appreciate&#9251;its&#9251;advantages.<br/>W1,W2: <i>/s</i>>]
            t005 [label=<W1: this<br/>W2: this&#9251;<br/>W1: <i>/s/app/rdg</i><br/>W2: <i>/s</i><br/>>]
            t006 [label=<W1: such&#9251;a<br/>W1: <i>/s/app/rdg</i>>]
            t007 [label=<W1: a<br/>W1: <i>/s/app/rdg</i>>]
            t000->t002[label="W1,W2"]
            t002->t005[label="W1,W2"]
            t002->t006[label="W1"]
            t002->t007[label="W1"]
            t003->t004[label="W1"]
            t004->t001[label="W1,W2"]
            t005->t003[label="W1"]
            t005->t004[label="W2"]
            t006->t003[label="W1"]
            t007->t003[label="W1"]
            }
            """.trimIndent()
        val expectedTable = """
            ┌────┬────────────────────────────────┬──────────┬─┬─────────────────────────────────────┐
            │[W1]│                                │<3>      a│ │                                     │
            │    │                                │<2> such a│ │                                     │
            │    │One must have lived longer with │<1> this  │ │system, to appreciate its advantages.│
            ├────┼────────────────────────────────┼──────────┼─┼─────────────────────────────────────┤
            │[W2]│One must have lived longer with │this      │ │system, to appreciate its advantages.│
            └────┴────────────────────────────────┴──────────┴─┴─────────────────────────────────────┘
            """.trimIndent()
        val collationGraph = testHyperCollation(wF, wQ, expectedDot, expectedTable)
    }

    @Test(timeout = 10000)
    fun testHierarchyWith3Witnesses() {
        val importer = XMLImporter()
        val wF = importer.importXML(
                "F",
                """
                |<text>
                |    <s>Hoe zoet moet nochtans zijn dit <lb/><del>werven om</del><add>trachten naar</add> een vrouw,
                |        de ongewisheid vóór de <lb/>liefelijke toestemming!</s>
                |</text>
                """.trimMargin())
        val wQ = importer.importXML(
                "Q",
                """
                |<text>
                |    <s>Hoe zoet moet nochtans zijn dit <del>werven om</del><add>trachten naar</add> een <lb/>vrouw !
                |        Die dagen van nerveuze verwachting vóór de liefelijke toestemming.</s>
                |</text>
                """.trimMargin())
        val wZ = importer.importXML(
                "Z",
                """
                |<text>
                |    <s>Hoe zoet moet nochtans zijn dit trachten naar een vrouw !
                |        Die dagen van ongewisheid vóór de liefelijke toestemming.</s>
                |</text>
                """.trimMargin())
        val expectedDot = """
            digraph CollationGraph{
            labelloc=b
            t000 [label="";shape=doublecircle,rank=middle]
            t001 [label="";shape=doublecircle,rank=middle]
            t002 [label=<F,Q,Z: Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/>F,Q,Z: <i>/text/s</i>>]
            t003 [label=<F,Q: &#9251;<br/>F,Q: <i>/text/s</i>>]
            t004 [label=<F,Q,Z: een&#9251;<br/>F,Q,Z: <i>/text/s</i>>]
            t005 [label=<F: vrouw<br/>Q: vrouw&#9251;<br/>Z: vrouw&#9251;<br/>F,Q,Z: <i>/text/s</i>>]
            t006 [label=<F: ,&#x21A9;<br/>&#9251;de&#9251;<br/>F: <i>/text/s</i>>]
            t007 [label=<F,Z: ongewisheid&#9251;<br/>F,Z: <i>/text/s</i>>]
            t008 [label=<F,Q,Z: vóór&#9251;de&#9251;<br/>F,Q,Z: <i>/text/s</i>>]
            t009 [label=<F: <br/>F: <i>/text/s/lb</i>>]
            t010 [label=<F,Q,Z: liefelijke&#9251;toestemming<br/>F,Q,Z: <i>/text/s</i>>]
            t011 [label=<F: !<br/>F: <i>/text/s</i>>]
            t012 [label=<F: <br/>F: <i>/text/s/lb</i>>]
            t013 [label=<F,Q: werven&#9251;om<br/>F,Q: <i>/text/s/del</i>>]
            t014 [label=<F: trachten&#9251;naar<br/>Q: trachten&#9251;naar<br/>Z: trachten&#9251;naar&#9251;<br/>F: <i>/text/s/add</i><br/>Q: <i>/text/s/add</i><br/>Z: <i>/text/s</i><br/>>]
            t015 [label=<Q: <br/>Q: <i>/text/s/lb</i>>]
            t016 [label=<Q: !&#x21A9;<br/>&#9251;Die&#9251;dagen&#9251;van&#9251;<br/>Z: !Die&#9251;dagen&#9251;van&#9251;<br/>Q,Z: <i>/text/s</i>>]
            t017 [label=<Q: nerveuze&#9251;verwachting&#9251;<br/>Q: <i>/text/s</i>>]
            t018 [label=<Q,Z: .<br/>Q,Z: <i>/text/s</i>>]
            t000->t002[label="F,Q,Z"]
            t002->t012[label="F"]
            t002->t013[label="Q"]
            t002->t014[label="Q,Z"]
            t003->t004[label="F,Q"]
            t004->t005[label="F,Z"]
            t004->t015[label="Q"]
            t005->t006[label="F"]
            t005->t016[label="Q,Z"]
            t006->t007[label="F"]
            t007->t008[label="F,Z"]
            t008->t009[label="F"]
            t008->t010[label="Q,Z"]
            t009->t010[label="F"]
            t010->t011[label="F"]
            t010->t018[label="Q,Z"]
            t011->t001[label="F"]
            t012->t013[label="F"]
            t012->t014[label="F"]
            t013->t003[label="F,Q"]
            t014->t003[label="F,Q"]
            t014->t004[label="Z"]
            t015->t005[label="Q"]
            t016->t007[label="Z"]
            t016->t017[label="Q"]
            t017->t008[label="Q"]
            t018->t001[label="Q,Z"]
            }
            """.trimIndent()
        val expectedTable = """
            ┌───┬────────────────────────────────┬─────┬─────────────────┬─┬────┬─────┬──────┬────────────────┬─────────────────────┬────────┬─────┬──────────────────────┬─┐
            │[F]│                                │     │[+] trachten_naar│ │    │     │      │                │                     │        │     │                      │ │
            │   │Hoe_zoet_moet_nochtans_zijn_dit_│<lb/>│[-] werven_om    │_│een_│     │vrouw │,_de_           │ongewisheid_         │vóór_de_│<lb/>│liefelijke_toestemming│!│
            ├───┼────────────────────────────────┼─────┼─────────────────┼─┼────┼─────┼──────┼────────────────┼─────────────────────┼────────┼─────┼──────────────────────┼─┤
            │[Q]│                                │     │[+] trachten_naar│ │    │     │      │                │                     │        │     │                      │ │
            │   │Hoe_zoet_moet_nochtans_zijn_dit_│     │[-] werven_om    │_│een_│<lb/>│vrouw_│!_Die_dagen_van_│nerveuze_verwachting_│vóór_de_│     │liefelijke_toestemming│.│
            ├───┼────────────────────────────────┼─────┼─────────────────┼─┼────┼─────┼──────┼────────────────┼─────────────────────┼────────┼─────┼──────────────────────┼─┤
            │[Z]│Hoe_zoet_moet_nochtans_zijn_dit_│     │trachten_naar_   │ │een_│     │vrouw_│!Die_dagen_van_ │ongewisheid_         │vóór_de_│     │liefelijke_toestemming│.│
            └───┴────────────────────────────────┴─────┴─────────────────┴─┴────┴─────┴──────┴────────────────┴─────────────────────┴────────┴─────┴──────────────────────┴─┘
            """.trimIndent()
        val collationGraph = testHyperCollation3(wF, wQ, wZ, expectedDot, expectedTable)

        // test matching tokens
        val n1 = CollationGraphAssert.textNodeSketch()
                .withWitnessSegmentSketch("F", "Hoe zoet moet nochtans zijn dit ")
                .withWitnessSegmentSketch("Q", "Hoe zoet moet nochtans zijn dit ")
                .withWitnessSegmentSketch("Z", "Hoe zoet moet nochtans zijn dit ")
        val n2 = CollationGraphAssert.textNodeSketch()
                .withWitnessSegmentSketch("F", " ")
                .withWitnessSegmentSketch("Q", " ")
        val n3 = CollationGraphAssert.textNodeSketch()
                .withWitnessSegmentSketch("F", "een ")
                .withWitnessSegmentSketch("Q", "een ")
                .withWitnessSegmentSketch("Z", "een ")
        val n4 = CollationGraphAssert.textNodeSketch()
                .withWitnessSegmentSketch("F", "vrouw")
                .withWitnessSegmentSketch("Q", "vrouw ")
                .withWitnessSegmentSketch("Z", "vrouw ")
        val n5 = CollationGraphAssert.textNodeSketch()
                .withWitnessSegmentSketch("F", "ongewisheid ")
                .withWitnessSegmentSketch("Z", "ongewisheid ")
        val n6 = CollationGraphAssert.textNodeSketch()
                .withWitnessSegmentSketch("F", "liefelijke toestemming")
                .withWitnessSegmentSketch("Z", "liefelijke toestemming")
                .withWitnessSegmentSketch("Q", "liefelijke toestemming")
        val trachten_naar = CollationGraphAssert.textNodeSketch()
                .withWitnessSegmentSketch("F", "trachten naar")
                .withWitnessSegmentSketch("Q", "trachten naar")
                .withWitnessSegmentSketch("Z", "trachten naar ")
        val werven_om = CollationGraphAssert.textNodeSketch()
                .withWitnessSegmentSketch("F", "werven om")
                .withWitnessSegmentSketch("Q", "werven om")
        assertThat(collationGraph)
                .containsTextNodesMatching(n1, n2, n3, n4, n5, n6, trachten_naar, werven_om)
        assertThat(collationGraph)
                .containsMarkupNodesMatching(
                        CollationGraphAssert.markupNodeSketch("F", "text"),
                        CollationGraphAssert.markupNodeSketch("Q", "text"),
                        CollationGraphAssert.markupNodeSketch("Z", "text"))
        val f_del = CollationGraphAssert.markupNodeSketch("F", "del")
        val q_add = CollationGraphAssert.markupNodeSketch("Q", "add")
        assertThat(collationGraph).hasTextNodeMatching(werven_om).withMarkupNodesMatching(f_del)
        assertThat(collationGraph).hasMarkupNodeMatching(q_add).withTextNodesMatching(trachten_naar)
    }

    @Test(timeout = 10000)
    fun testHierarchy() {
        val importer = XMLImporter()
        val fXML = """
            |<text>
            |    <s>Hoe zoet moet nochtans zijn dit <lb/><del>werven om</del><add>trachten naar</add> een vrouw,
            |        de ongewisheid vóór de <lb/>liefelijke toestemming!</s>
            |</text>
            """.trimMargin()
        val wF = importer.importXML("F", fXML)
        val qXML = """
            |<text>
            |    <s>Hoe zoet moet nochtans zijn dit <del>werven om</del><add>trachten naar</add> een <lb/>vrouw !
            |        Die dagen van nerveuze verwachting vóór de liefelijke toestemming.</s>
            |</text>
            """.trimMargin()
        val wQ = importer.importXML("Q", qXML)
        LOG.info(fXML)
        LOG.info(qXML)
        val expectedDotF = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            F_000 [label=<Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/><i>F: /text/s</i>>]
            F_006 [label=<<br/><i>F: /text/s/lb</i>>]
            F_007 [label=<werven&#9251;om<br/><i>F: /text/s/del</i>>]
            F_009 [label=<trachten&#9251;naar<br/><i>F: /text/s/add</i>>]
            F_011 [label=<&#9251;een&#9251;vrouw,&#x21A9;<br/>&#9251;de&#9251;ongewisheid&#9251;vóór&#9251;de&#9251;<br/><i>F: /text/s</i>>]
            F_019 [label=<<br/><i>F: /text/s/lb</i>>]
            F_020 [label=<liefelijke&#9251;toestemming!<br/><i>F: /text/s</i>>]
            end [label="";shape=doublecircle,rank=middle]
            F_000->F_006
            F_006->F_007
            F_006->F_009
            F_007->F_011
            F_009->F_011
            F_011->F_019
            F_019->F_020
            F_020->end
            begin->F_000
            }
            """.trimIndent()
        verifyDotExport(wF, expectedDotF)
        val expectedDotQ = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            Q_000 [label=<Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/><i>Q: /text/s</i>>]
            Q_006 [label=<werven&#9251;om<br/><i>Q: /text/s/del</i>>]
            Q_008 [label=<trachten&#9251;naar<br/><i>Q: /text/s/add</i>>]
            Q_010 [label=<&#9251;een&#9251;<br/><i>Q: /text/s</i>>]
            Q_012 [label=<<br/><i>Q: /text/s/lb</i>>]
            Q_013 [label=<vrouw&#9251;!&#x21A9;<br/>&#9251;Die&#9251;dagen&#9251;van&#9251;nerveuze&#9251;verwachting&#9251;vóór&#9251;de&#9251;liefelijke&#9251;toestemming.<br/><i>Q: /text/s</i>>]
            end [label="";shape=doublecircle,rank=middle]
            Q_000->Q_006
            Q_000->Q_008
            Q_006->Q_010
            Q_008->Q_010
            Q_010->Q_012
            Q_012->Q_013
            Q_013->end
            begin->Q_000
            }
            """.trimIndent()
        verifyDotExport(wQ, expectedDotQ)
        val expectedDot = """
            digraph CollationGraph{
            labelloc=b
            t000 [label="";shape=doublecircle,rank=middle]
            t001 [label="";shape=doublecircle,rank=middle]
            t002 [label=<F,Q: Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/>F,Q: <i>/text/s</i>>]
            t003 [label=<F,Q: &#9251;een&#9251;<br/>F,Q: <i>/text/s</i>>]
            t004 [label=<F: vrouw<br/>Q: vrouw&#9251;<br/>F,Q: <i>/text/s</i>>]
            t005 [label=<F: ,&#x21A9;<br/>&#9251;de&#9251;ongewisheid&#9251;<br/>F: <i>/text/s</i>>]
            t006 [label=<F,Q: vóór&#9251;de&#9251;<br/>F,Q: <i>/text/s</i>>]
            t007 [label=<F: <br/>F: <i>/text/s/lb</i>>]
            t008 [label=<F,Q: liefelijke&#9251;toestemming<br/>F,Q: <i>/text/s</i>>]
            t009 [label=<F: !<br/>F: <i>/text/s</i>>]
            t010 [label=<F: <br/>F: <i>/text/s/lb</i>>]
            t011 [label=<F,Q: werven&#9251;om<br/>F,Q: <i>/text/s/del</i>>]
            t012 [label=<F,Q: trachten&#9251;naar<br/>F,Q: <i>/text/s/add</i>>]
            t013 [label=<Q: <br/>Q: <i>/text/s/lb</i>>]
            t014 [label=<Q: !&#x21A9;<br/>&#9251;Die&#9251;dagen&#9251;van&#9251;nerveuze&#9251;verwachting&#9251;<br/>Q: <i>/text/s</i>>]
            t015 [label=<Q: .<br/>Q: <i>/text/s</i>>]
            t000->t002[label="F,Q"]
            t002->t010[label="F"]
            t002->t011[label="Q"]
            t002->t012[label="Q"]
            t003->t004[label="F"]
            t003->t013[label="Q"]
            t004->t005[label="F"]
            t004->t014[label="Q"]
            t005->t006[label="F"]
            t006->t007[label="F"]
            t006->t008[label="Q"]
            t007->t008[label="F"]
            t008->t009[label="F"]
            t008->t015[label="Q"]
            t009->t001[label="F"]
            t010->t011[label="F"]
            t010->t012[label="F"]
            t011->t003[label="F,Q"]
            t012->t003[label="F,Q"]
            t013->t004[label="Q"]
            t014->t006[label="Q"]
            t015->t001[label="Q"]
            }
            """.trimIndent()
        val expectedTable = """
            ┌───┬────────────────────────────────┬─────┬─────────────────┬─────┬─────┬──────┬─────────────────────────────────────┬────────┬─────┬──────────────────────┬─┐
            │[F]│                                │     │[+] trachten naar│     │     │      │                                     │        │     │                      │ │
            │   │Hoe zoet moet nochtans zijn dit │<lb/>│[-] werven om    │een  │     │vrouw │, de ongewisheid                     │vóór de │<lb/>│liefelijke toestemming│!│
            ├───┼────────────────────────────────┼─────┼─────────────────┼─────┼─────┼──────┼─────────────────────────────────────┼────────┼─────┼──────────────────────┼─┤
            │[Q]│                                │     │[+] trachten naar│     │     │      │                                     │        │     │                      │ │
            │   │Hoe zoet moet nochtans zijn dit │     │[-] werven om    │een  │<lb/>│vrouw │! Die dagen van nerveuze verwachting │vóór de │     │liefelijke toestemming│.│
            └───┴────────────────────────────────┴─────┴─────────────────┴─────┴─────┴──────┴─────────────────────────────────────┴────────┴─────┴──────────────────────┴─┘
            """.trimIndent()

        val collationGraph = testHyperCollation(wF, wQ, expectedDot, expectedTable)

        // test matching tokens
        assertThat(collationGraph)
                .containsTextNodesMatching(
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("F", "Hoe zoet moet nochtans zijn dit ")
                                .withWitnessSegmentSketch("Q", "Hoe zoet moet nochtans zijn dit "),
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("F", "trachten naar")
                                .withWitnessSegmentSketch("Q", "trachten naar"),
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("F", "werven om")
                                .withWitnessSegmentSketch("Q", "werven om"),
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("F", " een ")
                                .withWitnessSegmentSketch("Q", " een "),
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("F", "vrouw")
                                .withWitnessSegmentSketch("Q", "vrouw "),
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("F", "vóór de ")
                                .withWitnessSegmentSketch("Q", "vóór de "),
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("F", "liefelijke toestemming")
                                .withWitnessSegmentSketch("Q", "liefelijke toestemming"))
    }

    @Test(timeout = 10000)
    fun testOrder() {
        val importer = XMLImporter()
        val wF = importer.importXML(
                "F",
                """
                |<?xml version="1.0" encoding="UTF-8"?>
                |<text>
                |    <s>De vent was woedend en maakte <del type="instantCorrection">Shiriar</del> den bedremmelden
                |        Sultan uit voor "lompen boer".</s>
                |</text>
                """.trimMargin())
        val wQ = importer.importXML(
                "Q",
                """
                |<?xml version="1.0" encoding="UTF-8"?>
                |<text>
                |    <s>De vent was woedend en maakte <del>Shiriar</del>
                |        <add>den bedremmelden <del>man</del>
                |            <add>Sultan</add></add> uit voor "lompen boer".</s>
                |</text>
                """.trimMargin())
        val expectedDotF = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            F_000 [label=<De&#9251;vent&#9251;was&#9251;woedend&#9251;en&#9251;maakte&#9251;<br/><i>F: /text/s</i>>]
            F_006 [label=<Shiriar<br/><i>F: /text/s/del</i>>]
            F_007 [label=<&#9251;den&#9251;bedremmelden&#x21A9;<br/>&#9251;Sultan&#9251;uit&#9251;voor&#9251;"lompen&#9251;boer".<br/><i>F: /text/s</i>>]
            end [label="";shape=doublecircle,rank=middle]
            F_000->F_006
            F_000->F_007
            F_006->F_007
            F_007->end
            begin->F_000
            }
            """.trimIndent()
        verifyDotExport(wF, expectedDotF)
        val expectedDotQ = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            Q_000 [label=<De&#9251;vent&#9251;was&#9251;woedend&#9251;en&#9251;maakte&#9251;<br/><i>Q: /text/s</i>>]
            Q_006 [label=<Shiriar<br/><i>Q: /text/s/del</i>>]
            Q_007 [label=<den&#9251;bedremmelden&#9251;<br/><i>Q: /text/s/add</i>>]
            Q_011 [label=<&#9251;uit&#9251;voor&#9251;"lompen&#9251;boer".<br/><i>Q: /text/s</i>>]
            Q_009 [label=<man<br/><i>Q: /text/s/add/del</i>>]
            Q_010 [label=<Sultan<br/><i>Q: /text/s/add/add</i>>]
            end [label="";shape=doublecircle,rank=middle]
            Q_000->Q_006
            Q_000->Q_007
            Q_006->Q_011
            Q_007->Q_009
            Q_007->Q_010
            Q_009->Q_011
            Q_010->Q_011
            Q_011->end
            begin->Q_000
            }
            """.trimIndent()
        verifyDotExport(wQ, expectedDotQ)
        val expectedDot = """
            digraph CollationGraph{
            labelloc=b
            t000 [label="";shape=doublecircle,rank=middle]
            t001 [label="";shape=doublecircle,rank=middle]
            t002 [label=<F,Q: De&#9251;vent&#9251;was&#9251;woedend&#9251;en&#9251;maakte&#9251;<br/>F,Q: <i>/text/s</i>>]
            t003 [label=<F: Sultan&#9251;<br/>Q: Sultan<br/>F: <i>/text/s</i><br/>Q: <i>/text/s/add/add</i><br/>>]
            t004 [label=<F,Q: uit&#9251;voor&#9251;"lompen&#9251;boer".<br/>F,Q: <i>/text/s</i>>]
            t005 [label=<F,Q: Shiriar<br/>F,Q: <i>/text/s/del</i>>]
            t006 [label=<F: &#9251;<br/>F: <i>/text/s</i>>]
            t007 [label=<F: den&#9251;bedremmelden&#x21A9;<br/>&#9251;<br/>Q: den&#9251;bedremmelden&#9251;<br/>F: <i>/text/s</i><br/>Q: <i>/text/s/add</i><br/>>]
            t008 [label=<Q: &#9251;<br/>Q: <i>/text/s</i>>]
            t009 [label=<Q: man<br/>Q: <i>/text/s/add/del</i>>]
            t000->t002[label="F,Q"]
            t002->t005[label="F,Q"]
            t002->t006[label="F"]
            t002->t007[label="Q"]
            t003->t004[label="F"]
            t003->t008[label="Q"]
            t004->t001[label="F,Q"]
            t005->t006[label="F"]
            t005->t008[label="Q"]
            t006->t007[label="F"]
            t007->t003[label="F,Q"]
            t007->t009[label="Q"]
            t008->t004[label="Q"]
            t009->t008[label="Q"]
            }
            """.trimIndent()
        val expectedTable = """
            ┌───┬──────────────────────────────┬───────────┬─┬─────────────────────┬──────────┬─┬───────────────────────┐
            │[F]│De vent was woedend en maakte │[-] Shiriar│ │den bedremmelden     │Sultan    │ │uit voor "lompen boer".│
            ├───┼──────────────────────────────┼───────────┼─┼─────────────────────┼──────────┼─┼───────────────────────┤
            │[Q]│                              │           │ │                     │[+] Sultan│ │                       │
            │   │De vent was woedend en maakte │[-] Shiriar│ │[+] den bedremmelden │[-] man   │ │uit voor "lompen boer".│
            └───┴──────────────────────────────┴───────────┴─┴─────────────────────┴──────────┴─┴───────────────────────┘
            """.trimIndent()
        val collationGraph = testHyperCollation(wF, wQ, expectedDot, expectedTable)
        assertThat(collationGraph)
                .containsTextNodesMatching(
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("F", "De vent was woedend en maakte ")
                                .withWitnessSegmentSketch("Q", "De vent was woedend en maakte "),
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("F", "Shiriar")
                                .withWitnessSegmentSketch("Q", "Shiriar"),
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("F", "den bedremmelden\n        ")
                                .withWitnessSegmentSketch("Q", "den bedremmelden "),
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("F", "Sultan ")
                                .withWitnessSegmentSketch("Q", "Sultan"),
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("F", "uit voor \"lompen boer\".")
                                .withWitnessSegmentSketch("Q", "uit voor \"lompen boer\"."))
    }

    @Test
    fun testTheDog() {
        val importer = XMLImporter()
        val wF = importer.importXML("A", "<text>The dog's big eyes.</text>")
        val wQ = importer.importXML(
                "B", "<text>The dog's <del>big black ears</del><add>brown eyes</add>.</text>")
        val expectedDot = """
            digraph CollationGraph{
            labelloc=b
            t000 [label="";shape=doublecircle,rank=middle]
            t001 [label="";shape=doublecircle,rank=middle]
            t002 [label=<A,B: The&#9251;dog's&#9251;<br/>A,B: <i>/text</i>>]
            t003 [label=<A,B: big&#9251;<br/>A: <i>/text</i><br/>B: <i>/text/del</i><br/>>]
            t004 [label=<A,B: eyes<br/>A: <i>/text</i><br/>B: <i>/text/add</i><br/>>]
            t005 [label=<A,B: .<br/>A,B: <i>/text</i>>]
            t006 [label=<B: black&#9251;ears<br/>B: <i>/text/del</i>>]
            t007 [label=<B: brown&#9251;<br/>B: <i>/text/add</i>>]
            t000->t002[label="A,B"]
            t002->t003[label="A,B"]
            t002->t007[label="B"]
            t003->t004[label="A"]
            t003->t006[label="B"]
            t004->t005[label="A,B"]
            t005->t001[label="A,B"]
            t006->t005[label="B"]
            t007->t004[label="B"]
            }
            """.trimIndent()
        val expectedTable = """
            ┌───┬──────────┬──────────┬──────────────┬─┐
            │[A]│The dog's │big       │eyes          │.│
            ├───┼──────────┼──────────┼──────────────┼─┤
            │[B]│          │[+]  brown│[+]       eyes│ │
            │   │The dog's │[-] big   │[-] black ears│.│
            └───┴──────────┴──────────┴──────────────┴─┘
            """.trimIndent()
        val collationGraph = testHyperCollation(wF, wQ, expectedDot, expectedTable)
        assertThat(collationGraph)
                .containsOnlyTextNodesMatching(
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("A", "The dog's ")
                                .withWitnessSegmentSketch("B", "The dog's "),
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("A", "big ")
                                .withWitnessSegmentSketch("B", "big "),
                        CollationGraphAssert.textNodeSketch().withWitnessSegmentSketch("B", "black ears"),
                        CollationGraphAssert.textNodeSketch().withWitnessSegmentSketch("B", "brown "),
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("A", "eyes")
                                .withWitnessSegmentSketch("B", "eyes"),
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("A", ".")
                                .withWitnessSegmentSketch("B", "."))
    }

    @Test(timeout = 10000)
    fun testTranspositionAndDuplication() {
        val importer = XMLImporter()
        val wF = importer.importXML("A", "<text>T b b b b b b b Y</text>")
        val wQ = importer.importXML("B", "<text>X b b b b b b b T</text>")
        val expectedDot = """
            digraph CollationGraph{
            labelloc=b
            t000 [label="";shape=doublecircle,rank=middle]
            t001 [label="";shape=doublecircle,rank=middle]
            t002 [label=<A: T&#9251;<br/>A: <i>/text</i>>]
            t003 [label=<A,B: b&#9251;b&#9251;b&#9251;b&#9251;b&#9251;b&#9251;b&#9251;<br/>A,B: <i>/text</i>>]
            t004 [label=<A: Y<br/>A: <i>/text</i>>]
            t005 [label=<B: X&#9251;<br/>B: <i>/text</i>>]
            t006 [label=<B: T<br/>B: <i>/text</i>>]
            t000->t002[label="A"]
            t000->t005[label="B"]
            t002->t003[label="A"]
            t003->t004[label="A"]
            t003->t006[label="B"]
            t004->t001[label="A"]
            t005->t003[label="B"]
            t006->t001[label="B"]
            }
            """.trimIndent()
        val expectedTable = """
            ┌───┬──┬──────────────┬─┐
            │[A]│T │b b b b b b b │Y│
            ├───┼──┼──────────────┼─┤
            │[B]│X │b b b b b b b │T│
            └───┴──┴──────────────┴─┘
            """.trimIndent()
        val collationGraph = testHyperCollation(wF, wQ, expectedDot, expectedTable)
        assertThat(collationGraph)
                .containsTextNodesMatching(
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("A", "b b b b b b b ")
                                .withWitnessSegmentSketch("B", "b b b b b b b "))
        assertThat(collationGraph)
                .doesNotContainTextNodesMatching(
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("A", "T ")
                                .withWitnessSegmentSketch("B", "X "))
    }

    @Test(timeout = 10000)
    fun testDoubleTransposition() {
        val importer = XMLImporter()
        val wF = importer.importXML("A", "<text>A b C d E C f G H</text>")
        val wQ = importer.importXML("B", "<text>A H i j E C G k</text>")
        val expectedDot = """
            digraph CollationGraph{
            labelloc=b
            t000 [label="";shape=doublecircle,rank=middle]
            t001 [label="";shape=doublecircle,rank=middle]
            t002 [label=<A,B: A&#9251;<br/>A,B: <i>/text</i>>]
            t003 [label=<A: b&#9251;C&#9251;d&#9251;<br/>A: <i>/text</i>>]
            t004 [label=<A,B: E&#9251;C&#9251;<br/>A,B: <i>/text</i>>]
            t005 [label=<A: f&#9251;<br/>A: <i>/text</i>>]
            t006 [label=<A,B: G&#9251;<br/>A,B: <i>/text</i>>]
            t007 [label=<A: H<br/>A: <i>/text</i>>]
            t008 [label=<B: H&#9251;i&#9251;j&#9251;<br/>B: <i>/text</i>>]
            t009 [label=<B: k<br/>B: <i>/text</i>>]
            t000->t002[label="A,B"]
            t002->t003[label="A"]
            t002->t008[label="B"]
            t003->t004[label="A"]
            t004->t005[label="A"]
            t004->t006[label="B"]
            t005->t006[label="A"]
            t006->t007[label="A"]
            t006->t009[label="B"]
            t007->t001[label="A"]
            t008->t004[label="B"]
            t009->t001[label="B"]
            }
            """.trimIndent()
        val expectedTable = """
            ┌───┬──┬──────┬────┬──┬──┬─┐
            │[A]│A │b C d │E C │f │G │H│
            ├───┼──┼──────┼────┼──┼──┼─┤
            │[B]│A │H i j │E C │  │G │k│
            └───┴──┴──────┴────┴──┴──┴─┘
            """.trimIndent()
        val collationGraph = testHyperCollation(wF, wQ, expectedDot, expectedTable)
        assertThat(collationGraph)
                .containsTextNodesMatching(
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("A", "A ")
                                .withWitnessSegmentSketch("B", "A "),
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("A", "E C ")
                                .withWitnessSegmentSketch("B", "E C "),
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("A", "G ")
                                .withWitnessSegmentSketch("B", "G "))
        assertThat(collationGraph)
                .doesNotContainTextNodesMatching(
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("A", "H")
                                .withWitnessSegmentSketch("B", "H "))
    }

    @Test(timeout = 10000)
    fun testVirginiaWoolfTimePassesFragment() {
        val importer = XMLImporter()
        val xml1 = """
            <text>
            <div n="2">
            <s>Leaning her bony breast on the hard thorn she crooned out her forgiveness.</s>
            </div>
            <div n="3">
            <s>Was it then that she had her consolations  </s>
            </div>
            </text>
            """.trimIndent()
        LOG.info("H: {}", xml1)
        val wF = importer.importXML("H", xml1)
        val xml2 = """
            <text>
            <p>
            <s> granting, as she stood the chair straight by the dressing table, <add>leaning her bony breast on the hard thorn</add>, her forgiveness of it all.</s>
            </p>
            <p>
            <s>Was it then that she had her consolations ... </s>
            </p>
            </text>
            """.trimIndent()
        LOG.info("T: {}", xml2)
        val wQ = importer.importXML("T", xml2)
        val expectedDot = """
            digraph CollationGraph{
            labelloc=b
            t000 [label="";shape=doublecircle,rank=middle]
            t001 [label="";shape=doublecircle,rank=middle]
            t002 [label=<H: Leaning&#9251;her&#9251;bony&#9251;breast&#9251;on&#9251;the&#9251;hard&#9251;thorn&#9251;<br/>T: leaning&#9251;her&#9251;bony&#9251;breast&#9251;on&#9251;the&#9251;hard&#9251;thorn<br/>H: <i>/text/div/s</i><br/>T: <i>/text/p/s/add</i><br/>>]
            t003 [label=<H: her&#9251;forgiveness<br/>T: her&#9251;forgiveness&#9251;<br/>H: <i>/text/div/s</i><br/>T: <i>/text/p/s</i><br/>>]
            t004 [label=<H,T: .Was&#9251;it&#9251;then&#9251;that&#9251;she&#9251;had&#9251;her&#9251;consolations&#9251;<br/>H: <i>/text/div/s</i><br/>T: <i>/text/p/s</i><br/>>]
            t005 [label=<H: she&#9251;crooned&#9251;out&#9251;<br/>H: <i>/text/div/s</i>>]
            t006 [label=<T: &#9251;granting,&#9251;as&#9251;she&#9251;stood&#9251;the&#9251;chair&#9251;straight&#9251;by&#9251;the&#9251;dressing&#9251;table,&#9251;<br/>T: <i>/text/p/s</i>>]
            t007 [label=<T: ,&#9251;<br/>T: <i>/text/p/s</i>>]
            t008 [label=<T: of&#9251;it&#9251;all<br/>T: <i>/text/p/s</i>>]
            t009 [label=<T: ...&#9251;<br/>T: <i>/text/p/s</i>>]
            t000->t002[label="H"]
            t000->t006[label="T"]
            t002->t005[label="H"]
            t002->t007[label="T"]
            t003->t004[label="H"]
            t003->t008[label="T"]
            t004->t001[label="H"]
            t004->t009[label="T"]
            t005->t003[label="H"]
            t006->t002[label="T"]
            t006->t007[label="T"]
            t007->t003[label="T"]
            t008->t004[label="T"]
            t009->t001[label="T"]
            }
            """.trimIndent()
        val expectedTable = """
            ┌───┬──────────────────────────────────────────────────────────────────┬─────────────────────────────────────────────┬────────────────┬────────────────┬─────────┬───────────────────────────────────────────┬────┐
            │[H]│                                                                  │Leaning her bony breast on the hard thorn    │she crooned out │her forgiveness │         │.Was it then that she had her consolations │    │
            ├───┼──────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────┼────────────────┼────────────────┼─────────┼───────────────────────────────────────────┼────┤
            │[T]│granting, as she stood the chair straight by the dressing table,  │[+] leaning her bony breast on the hard thorn│,               │her forgiveness │of it all│.Was it then that she had her consolations │... │
            └───┴──────────────────────────────────────────────────────────────────┴─────────────────────────────────────────────┴────────────────┴────────────────┴─────────┴───────────────────────────────────────────┴────┘
            """.trimIndent()
        val collationGraph = testHyperCollation(wF, wQ, expectedDot, expectedTable)
        assertThat(collationGraph)
                .containsTextNodesMatching(
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("H", "Leaning her bony breast on the hard thorn ")
                                .withWitnessSegmentSketch("T", "leaning her bony breast on the hard thorn"),
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("H", "her forgiveness")
                                .withWitnessSegmentSketch("T", "her forgiveness "),
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("H", ".Was it then that she had her consolations  ")
                                .withWitnessSegmentSketch("T", ".Was it then that she had her consolations "))
        assertThat(collationGraph)
                .doesNotContainTextNodesMatching(
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("H", ", ")
                                .withWitnessSegmentSketch("T", ", "))
    }

    @Test(timeout = 10000)
    fun testMaryShellyGodwinFrankensteinFragment1() {
        val importer = XMLImporter()
        val xmlN = """
            <text>
            <s>so destitute of every hope of consolation to live
            <del rend="strikethrough">-</del>
            <add place="overwritten" hand="#pbs">?</add> oh no - ...
            </s>
            </text>
            """.trimIndent()
        val wF = importer.importXML("N", xmlN)
        LOG.info("N: {}", xmlN)
        val xmlF = """
            <text>
            <p>
            <s>so infinitely miserable, so destitute of every hope of consolation to live?</s> <s>Oh, no! ... </s>
            </p></text>
            """.trimIndent()
        LOG.info("F: {}", xmlF)
        val wQ = importer.importXML("F", xmlF)
        val expectedDot = """
            digraph CollationGraph{
            labelloc=b
            t000 [label="";shape=doublecircle,rank=middle]
            t001 [label="";shape=doublecircle,rank=middle]
            t002 [label=<F,N: so&#9251;<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>]
            t003 [label=<F,N: ?<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s/add</i><br/>>]
            t004 [label=<F,N: &#9251;<br/>F: <i>/text/p</i><br/>N: <i>/text/s</i><br/>>]
            t005 [label=<F: Oh<br/>N: oh&#9251;<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>]
            t006 [label=<F: ,&#9251;<br/>F: <i>/text/p/s</i>>]
            t007 [label=<F: no<br/>N: no&#9251;<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>]
            t008 [label=<F: !&#9251;<br/>F: <i>/text/p/s</i>>]
            t009 [label=<F: ...&#9251;<br/>N: ...&#x21A9;<br/><br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>]
            t010 [label=<F: infinitely&#9251;miserable,&#9251;so&#9251;<br/>F: <i>/text/p/s</i>>]
            t011 [label=<F,N: destitute&#9251;of&#9251;every&#9251;hope&#9251;of&#9251;consolation&#9251;to&#9251;live<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>]
            t012 [label=<N: -&#9251;<br/>N: <i>/text/s</i>>]
            t013 [label=<N: -<br/>N: <i>/text/s/del</i>>]
            t000->t002[label="F,N"]
            t002->t010[label="F"]
            t002->t011[label="N"]
            t003->t004[label="F,N"]
            t004->t005[label="F,N"]
            t005->t006[label="F"]
            t005->t007[label="N"]
            t006->t007[label="F"]
            t007->t008[label="F"]
            t007->t012[label="N"]
            t008->t009[label="F"]
            t009->t001[label="F,N"]
            t010->t011[label="F"]
            t011->t003[label="F,N"]
            t011->t013[label="N"]
            t012->t009[label="N"]
            t013->t004[label="N"]
            }
            """.trimIndent()
        val expected1 = """
            digraph CollationGraph{
            labelloc=b
            t000 [label="";shape=doublecircle,rank=middle]
            t001 [label="";shape=doublecircle,rank=middle]
            t002 [label=<F: so&#9251;infinitely&#9251;miserable,&#9251;<br/>F: <i>/text/p/s</i>>]
            t003 [label=<F,N: ?<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s/add</i><br/>>]
            t004 [label=<F,N: &#9251;<br/>F: <i>/text/p</i><br/>N: <i>/text/s</i><br/>>]
            t005 [label=<F: Oh<br/>N: oh&#9251;<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>]
            t006 [label=<F: ,&#9251;<br/>F: <i>/text/p/s</i>>]
            t007 [label=<F: no<br/>N: no&#9251;<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>]
            t008 [label=<F: !&#9251;<br/>F: <i>/text/p/s</i>>]
            t009 [label=<F: ...&#9251;<br/>N: ...&#x21A9;<br/><br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>]
            t010 [label=<F,N: so&#9251;destitute&#9251;of&#9251;every&#9251;hope&#9251;of&#9251;consolation&#9251;to&#9251;live<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>]
            t011 [label=<N: -&#9251;<br/>N: <i>/text/s</i>>]
            t012 [label=<N: -<br/>N: <i>/text/s/del</i>>]
            t000->t002[label="F"]
            t000->t010[label="N"]
            t002->t010[label="F"]
            t003->t004[label="F,N"]
            t004->t005[label="F,N"]
            t005->t006[label="F"]
            t005->t007[label="N"]
            t006->t007[label="F"]
            t007->t008[label="F"]
            t007->t011[label="N"]
            t008->t009[label="F"]
            t009->t001[label="F,N"]
            t010->t003[label="F,N"]
            t010->t012[label="N"]
            t011->t009[label="N"]
            t012->t004[label="N"]
            }
            """.trimIndent()
        val expectedTable = """
            ┌───┬───┬─────────────────────────┬──────────────────────────────────────────────┬─────┬─┬───┬──┬───┬──┬────┐
            │[F]│so │infinitely miserable, so │destitute of every hope of consolation to live│?    │ │Oh │, │no │! │... │
            ├───┼───┼─────────────────────────┼──────────────────────────────────────────────┼─────┼─┼───┼──┼───┼──┼────┤
            │[N]│   │                         │                                              │[+] ?│ │   │  │   │  │    │
            │   │so │                         │destitute of every hope of consolation to live│[-] -│ │oh │  │no │- │... │
            └───┴───┴─────────────────────────┴──────────────────────────────────────────────┴─────┴─┴───┴──┴───┴──┴────┘
            """.trimIndent()
        val collationGraph = testHyperCollation(wF, wQ, expectedDot, expectedTable)
        assertThat(collationGraph)
                .containsTextNodesMatching(
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("F", "so ")
                                .withWitnessSegmentSketch("N", "so "),
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("F", "destitute of every hope of consolation to live")
                                .withWitnessSegmentSketch("N", "destitute of every hope of consolation to live"),
                        CollationGraphAssert.textNodeSketch().withWitnessSegmentSketch("F", " ").withWitnessSegmentSketch("N", " "),
                        CollationGraphAssert.textNodeSketch().withWitnessSegmentSketch("F", "?").withWitnessSegmentSketch("N", "?"),
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("F", "Oh")
                                .withWitnessSegmentSketch("N", "oh "),
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("F", "no")
                                .withWitnessSegmentSketch("N", "no "),
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("F", "... ")
                                .withWitnessSegmentSketch("N", "...\n"))
    }

    @Test(timeout = 10000)
    fun testMaryShellyGodwinFrankensteinFragment2() {
        val importer = XMLImporter()
        val xmlN = """
            <text>
            <s>Frankenstein discovered that I detailed or made notes concerning his history he asked to see them &amp; himself corrected
            <add place="superlinear">and augmented</add>
            them in many places</s>
            </text>
            """.trimIndent()
        val wF = importer.importXML("N", xmlN)
        LOG.info("N: {}", xmlN)
        val xmlF = """
            <text>
            <s>Frankenstein discovered
            <del rend="strikethrough">or</del>
            <add place="superlinear">that I</add> made notes concerning his history; he asked to see them and then himself corrected and augmented them in many places
            </s>
            </text>
            """.trimIndent()
        LOG.info("F: {}", xmlF)
        val wQ = importer.importXML("F", xmlF)
        val expectedDot = """
            digraph CollationGraph{
            labelloc=b
            t000 [label="";shape=doublecircle,rank=middle]
            t001 [label="";shape=doublecircle,rank=middle]
            t002 [label=<F: Frankenstein&#9251;discovered<br/>N: Frankenstein&#9251;discovered&#9251;<br/>F,N: <i>/text/s</i>>]
            t003 [label=<F: ;&#9251;<br/>F: <i>/text/s</i>>]
            t004 [label=<F,N: he&#9251;asked&#9251;to&#9251;see&#9251;them&#9251;<br/>F,N: <i>/text/s</i>>]
            t005 [label=<F: and&#9251;then&#9251;<br/>F: <i>/text/s</i>>]
            t006 [label=<F: himself&#9251;corrected&#9251;<br/>N: himself&#9251;corrected&#x21A9;<br/><br/>F,N: <i>/text/s</i>>]
            t007 [label=<F: and&#9251;augmented&#9251;<br/>N: and&#9251;augmented<br/>F: <i>/text/s</i><br/>N: <i>/text/s/add</i><br/>>]
            t008 [label=<F: them&#9251;in&#9251;many&#9251;places&#x21A9;<br/><br/>N: them&#9251;in&#9251;many&#9251;places<br/>F,N: <i>/text/s</i>>]
            t009 [label=<F: or<br/>N: or&#9251;<br/>F: <i>/text/s/del</i><br/>N: <i>/text/s</i><br/>>]
            t010 [label=<F: that&#9251;I<br/>N: that&#9251;I&#9251;<br/>F: <i>/text/s/add</i><br/>N: <i>/text/s</i><br/>>]
            t011 [label=<F: &#9251;<br/>F: <i>/text/s</i>>]
            t012 [label=<F: made&#9251;notes&#9251;concerning&#9251;his&#9251;history<br/>N: made&#9251;notes&#9251;concerning&#9251;his&#9251;history&#9251;<br/>F,N: <i>/text/s</i>>]
            t013 [label=<N: &amp;&#9251;<br/>N: <i>/text/s</i>>]
            t014 [label=<N: detailed&#9251;<br/>N: <i>/text/s</i>>]
            t000->t002[label="F,N"]
            t002->t009[label="F"]
            t002->t010[label="F,N"]
            t003->t004[label="F"]
            t004->t005[label="F"]
            t004->t013[label="N"]
            t005->t006[label="F"]
            t006->t007[label="F,N"]
            t006->t008[label="N"]
            t007->t008[label="F,N"]
            t008->t001[label="F,N"]
            t009->t011[label="F"]
            t009->t012[label="N"]
            t010->t011[label="F"]
            t010->t014[label="N"]
            t011->t012[label="F"]
            t012->t003[label="F"]
            t012->t004[label="N"]
            t013->t006[label="N"]
            t014->t009[label="N"]
            }
            """.trimIndent()
        val expectedTable = """
            ┌───┬────────────────────────┬──────────┬─────────┬──────┬─┬──────────────────────────────────┬──┬─────────────────────┬─────────┬──────────────────┬─────────────────┬────────────────────┐
            │[F]│Frankenstein discovered │[+] that I│         │[-] or│ │made notes concerning his history │; │he asked to see them │and then │himself corrected │and augmented    │them in many places │
            ├───┼────────────────────────┼──────────┼─────────┼──────┼─┼──────────────────────────────────┼──┼─────────────────────┼─────────┼──────────────────┼─────────────────┼────────────────────┤
            │[N]│Frankenstein discovered │that I    │detailed │or    │ │made notes concerning his history │  │he asked to see them │&        │himself corrected │[+] and augmented│them in many places │
            └───┴────────────────────────┴──────────┴─────────┴──────┴─┴──────────────────────────────────┴──┴─────────────────────┴─────────┴──────────────────┴─────────────────┴────────────────────┘
            """.trimIndent()
        val collationGraph = testHyperCollation(wF, wQ, expectedDot, expectedTable)
        assertThat(collationGraph)
                .containsTextNodesMatching(
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("F", "Frankenstein discovered")
                                .withWitnessSegmentSketch("N", "Frankenstein discovered "),
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("F", "that I")
                                .withWitnessSegmentSketch("N", "that I "),
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("F", "or")
                                .withWitnessSegmentSketch("N", "or "),
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("F", "made notes concerning his history")
                                .withWitnessSegmentSketch("N", "made notes concerning his history "),
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("F", "he asked to see them ")
                                .withWitnessSegmentSketch("F", "he asked to see them ")
                                .withWitnessSegmentSketch("N", "he asked to see them "),
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("F", "himself corrected ")
                                .withWitnessSegmentSketch("N", "himself corrected\n"),
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("F", "and augmented ")
                                .withWitnessSegmentSketch("N", "and augmented"),
                        CollationGraphAssert.textNodeSketch()
                                .withWitnessSegmentSketch("F", "them in many places\n")
                                .withWitnessSegmentSketch("N", "them in many places"))
    }

    @Test(timeout = 10000)
    fun testCollationGraphInitialization() {
        val importer = XMLImporter()
        val wF = importer.importXML(
                "F",
                """
                |<text>
                |    <s>Hoe zoet moet nochtans zijn dit <lb/><del>werven om</del><add>trachten naar</add> een vrouw,
                |        de ongewisheid vóór de <lb/>liefelijke toestemming!</s>
                |</text>
                """.trimMargin())
        val collationGraph = CollationGraph()
        val map: MutableMap<TokenVertex, TextNode> = mutableMapOf()
        val markupNodeIndex: MutableMap<Markup, MarkupNode> = mutableMapOf()
        hyperCollator.initialize(collationGraph, map, markupNodeIndex, wF)
        val collation = CollationGraphNodeJoiner.join(collationGraph)
        val dot = CollationGraphVisualizer.toDot(
                collation,
                emphasizeWhitespace = true,
                hideMarkup = false
        )
        val expected = """
            digraph CollationGraph{
            labelloc=b
            t000 [label="";shape=doublecircle,rank=middle]
            t001 [label="";shape=doublecircle,rank=middle]
            t002 [label=<F: Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/>F: <i>/text/s</i>>]
            t003 [label=<F: &#9251;een&#9251;vrouw,&#x21A9;<br/>&#9251;de&#9251;ongewisheid&#9251;vóór&#9251;de&#9251;<br/>F: <i>/text/s</i>>]
            t004 [label=<F: <br/>F: <i>/text/s/lb</i>>]
            t005 [label=<F: liefelijke&#9251;toestemming!<br/>F: <i>/text/s</i>>]
            t006 [label=<F: <br/>F: <i>/text/s/lb</i>>]
            t007 [label=<F: werven&#9251;om<br/>F: <i>/text/s/del</i>>]
            t008 [label=<F: trachten&#9251;naar<br/>F: <i>/text/s/add</i>>]
            t000->t002[label="F"]
            t002->t006[label="F"]
            t003->t004[label="F"]
            t004->t005[label="F"]
            t005->t001[label="F"]
            t006->t007[label="F"]
            t006->t008[label="F"]
            t007->t003[label="F"]
            t008->t003[label="F"]
            }
            """.trimIndent()
        assertThat(dot).isEqualTo(expected)
        val dotWithoutMarkupAndWhitespaceEmphasis = CollationGraphVisualizer.toDot(
                collation,
                emphasizeWhitespace = false,
                hideMarkup = true
        )
        val expected2 = """
            digraph CollationGraph{
            labelloc=b
            t000 [label="";shape=doublecircle,rank=middle]
            t001 [label="";shape=doublecircle,rank=middle]
            t002 [label=<F: Hoe&nbsp;zoet&nbsp;moet&nbsp;nochtans&nbsp;zijn&nbsp;dit&nbsp;>]
            t003 [label=<F: &nbsp;een&nbsp;vrouw,&#x21A9;<br/>&nbsp;de&nbsp;ongewisheid&nbsp;vóór&nbsp;de&nbsp;>]
            t004 [label=<F: >]
            t005 [label=<F: liefelijke&nbsp;toestemming!>]
            t006 [label=<F: >]
            t007 [label=<F: werven&nbsp;om>]
            t008 [label=<F: trachten&nbsp;naar>]
            t000->t002[label="F"]
            t002->t006[label="F"]
            t003->t004[label="F"]
            t004->t005[label="F"]
            t005->t001[label="F"]
            t006->t007[label="F"]
            t006->t008[label="F"]
            t007->t003[label="F"]
            t008->t003[label="F"]
            }
            """.trimIndent()
        assertThat(dotWithoutMarkupAndWhitespaceEmphasis).isEqualTo(expected2)

        // System.out.println(dot);
        // writeGraph(dot, "graph");
    }

    @Test(timeout = 10000)
    fun testPermute() {
        val permute1 = hyperCollator.permute(3)
        LOG.info("permute={}", visualize(permute1))
        assertThat(Sets.newHashSet(permute1)).hasSameSizeAs(permute1)
        assertThat(permute1).hasSize(3)
        val permute2 = hyperCollator.permute(4)
        LOG.info("permute={}", visualize(permute2))
        assertThat(Sets.newHashSet(permute2)).hasSameSizeAs(permute2)
        assertThat(permute2).hasSize(6)
        val permute3 = hyperCollator.permute(10)
        LOG.info("permute={}", visualize(permute3))
        assertThat(Sets.newHashSet(permute3)).hasSameSizeAs(permute3)
        assertThat(permute3).hasSize(45)
    }

    @Test(timeout = 10000)
    fun testPotentialMatches() {
        val importer = XMLImporter()
        val sigil1 = "A"
        val sigil2 = "B"
        val sigil3 = "C"
        val w1 = importer.importXML(sigil1, "<x>the black cat</x>")
        val w2 = importer.importXML(sigil2, "<x>the blue dog</x>")
        val w3 = importer.importXML(sigil3, "<x>the black dog</x>")
        val witnesses = listOf(w1, w2, w3)
        val rankings = witnesses.map { VariantWitnessGraphRanking.of(it) }
        val allPotentialMatches = hyperCollator.getPotentialMatches(witnesses, rankings)
        LOG.info("allPotentialMatches={}", allPotentialMatches)
        val match1 = "<A0,B0>"
        val match2 = "<A0,C0>"
        val match3 = "<A1,C1>"
        val match4 = "<B0,C0>"
        val match5 = "<B2,C2>"
        val match6 = "<A:EndTokenVertex,B:EndTokenVertex>"
        val match7 = "<A:EndTokenVertex,C:EndTokenVertex>"
        val match8 = "<B:EndTokenVertex,C:EndTokenVertex>"
        assertThat(allPotentialMatches).hasSize(8)
        val matchStrings = allPotentialMatches.map { it.toString() }.toSet()
        assertThat(matchStrings)
                .contains(match1, match2, match3, match4, match5, match6, match7, match8)
        val sortAndFilterMatchesByWitness = hyperCollator.sortAndFilterMatchesByWitness(
                allPotentialMatches, listOf(sigil1, sigil2, sigil3))
        LOG.info("sortAndFilterMatchesByWitness={}", sortAndFilterMatchesByWitness)
        assertThat(sortAndFilterMatchesByWitness).containsOnlyKeys(sigil1, sigil2, sigil3)
        val listA = stringList(sortAndFilterMatchesByWitness, sigil1)
        assertThat(listA).containsOnly(match1, match2, match3, match6, match7)
        val listB = stringList(sortAndFilterMatchesByWitness, sigil2)
        assertThat(listB).containsOnly(match4, match1, match5, match6, match8)
        val listC = stringList(sortAndFilterMatchesByWitness, sigil3)
        assertThat(listC).containsOnly(match4, match2, match3, match5, match7, match8)
    }

    private fun stringList(
            sortAndFilterMatchesByWitness: Map<String, List<Match>>,
            key: String
    ): List<String> =
            (sortAndFilterMatchesByWitness[key] ?: error("key $key not found in sortAndFilterMatchesByWitness"))
                    .map(Match::toString)

    private fun visualize(list: List<Tuple<Int>>): String =
            list.joinToString("") { format("<{0},{1}>", it.left, it.right) }

    private fun testHyperCollation(
            witness1: VariantWitnessGraph,
            witness2: VariantWitnessGraph,
            expectedDot: String,
            expectedTable: String
    ): CollationGraph {
        val stopwatch = Stopwatch.createStarted()
        val collation0 = hyperCollator.collate(witness1, witness2)
        stopwatch.stop()
        val duration = stopwatch.elapsed(TimeUnit.MILLISECONDS)
        LOG.info("Collating took {} ms.", duration)
        val collation = CollationGraphNodeJoiner.join(collation0)
        val dot = CollationGraphVisualizer.toDot(
                collation,
                emphasizeWhitespace = true,
                hideMarkup = false
        )
        LOG.debug("dot=\n{}", dot)
        writeGraph(dot, "graph")
        assertThat(dot).isEqualTo(expectedDot)

        val table = CollationGraphVisualizer.toTableASCII(collation, false)
        LOG.debug("table=\n{}", table)
        assertThat(table).isEqualTo(expectedTable.replace("\n", System.lineSeparator()))
        return collation
    }

    private fun testHyperCollation3(
            witness1: VariantWitnessGraph,
            witness2: VariantWitnessGraph,
            witness3: VariantWitnessGraph,
            expectedDot: String,
            expectedTable: String
    ): CollationGraph {
        //    Map<String, Long> collationDuration = new HashMap<>();
        val stopwatch = Stopwatch.createStarted()
        var collation = hyperCollator.collate(witness1, witness2, witness3)
        stopwatch.stop()
        val duration = stopwatch.elapsed(TimeUnit.MILLISECONDS)
        LOG.info("Collating took {} ms.", duration)
        val markupBeforeJoin = collation.markupStream.collect(Collectors.toSet())
        //    LOG.info("before join: collation markup = {}",
        // collation.getMarkupStream().map(Markup::toString).sorted().collect(toList()));
        collation = CollationGraphNodeJoiner.join(collation)
        val markupAfterJoin = collation.markupStream.collect(Collectors.toSet())
        //    LOG.info("after join: collation markup = {}",
        // collation.getMarkupStream().map(Markup::toString).sorted().collect(toList()));
        assertThat(markupAfterJoin).containsExactlyElementsOf(markupBeforeJoin)

        val dot = CollationGraphVisualizer.toDot(collation, true, false)
        LOG.info("dot=\n{}", dot)
        writeGraph(dot, "graph")
        assertThat(dot).isEqualTo(expectedDot)

        val table = CollationGraphVisualizer.toTableASCII(collation, true)
        LOG.info("table=\n{}", table)
        assertThat(table).isEqualTo(expectedTable.replace("\n", System.lineSeparator()))
        return collation
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(HyperCollateTest::class.java)
    }

}
