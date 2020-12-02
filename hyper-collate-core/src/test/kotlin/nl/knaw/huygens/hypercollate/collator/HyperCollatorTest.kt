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
import nl.knaw.huygens.hypercollate.tools.DotFactory
import org.assertj.core.api.SoftAssertions
import org.assertj.core.util.Sets
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.slf4j.LoggerFactory
import java.text.MessageFormat.format
import java.util.concurrent.TimeUnit

class HyperCollatorTest {

    @Nested
    inner class TwoWitnessTests : HyperCollateTest() {

        @Test
        @Timeout(15)
        fun trd_665() {
            val importer = XMLImporter()
            val wA = importer.importXML(
                    "A", "<xml>Des objets en voie de disparition je détourne <subst><del>les yeux</del><add><del instant=\"true\">les</del>mes regards</add></subst> bien à l'avance.</xml>")
            val wB = importer.importXML(
                    "B", "<xml>c'est bien à l'avance que je détourne mes regards.</xml>")
            log.info("wA={}", wA.asDot())
            val expectedDot = """
                digraph CollationGraph{
                labelloc=b
                t000 [label="";shape=doublecircle,rank=middle]
                t001 [label=<A: Des&#9251;objets&#9251;en&#9251;voie&#9251;de&#9251;disparition&#9251;<br/>A: <i>/xml</i>>]
                t002 [label="";shape=doublecircle,rank=middle]
                t003 [label=<B: c'est&#9251;bien&#9251;à&#9251;l'avance&#9251;que&#9251;<br/>B: <i>/xml</i>>]
                t004 [label=<A,B: je&#9251;détourne&#9251;<br/>A,B: <i>/xml</i>>;penwidth=2]
                t005 [label=<A<sup>-</sup>: les&#9251;yeux<br/>A: <i>/xml/subst/del</i>>]
                t006 [label=<A<sup>+-</sup>: les<br/>A: <i>/xml/subst/add/del</i>>]
                t007 [label=<A<sup>+</sup>,B: mes&#9251;regards<br/>A: <i>/xml/subst/add</i><br/>B: <i>/xml</i><br/>>;penwidth=2]
                t008 [label=<A: &#9251;bien&#9251;à&#9251;l'avance<br/>A: <i>/xml</i>>]
                t009 [label=<A,B: .<br/>A,B: <i>/xml</i>>;penwidth=2]
                t000->t001[label=<A>]
                t000->t003[label=<B>]
                t001->t004[label=<A>]
                t003->t004[label=<B>]
                t004->t005[label=<A<sup>-</sup>>]
                t004->t006[label=<A<sup>+-</sup>>]
                t004->t007[label=<A<sup>+</sup>,B>;penwidth=2]
                t005->t008[label=<A<sup>-</sup>>]
                t006->t008[label=<A<sup>+-</sup>>]
                t007->t008[label=<A<sup>+</sup>>]
                t007->t009[label=<B>]
                t008->t009[label=<A>]
                t009->t002[label=<A,B>;penwidth=2]
                }
                """.trimIndent()
            val expectedTable = """
                ┌───┬──────────────────────────────────┬────────────┬───────────────┬────────────────┬─┐
                │[A]│                                  │            │[+] mes regards│                │ │
                │   │                                  │            │[-] les        │                │ │
                │   │Des objets en voie de disparition │je détourne │[-] les yeux   │bien à l'avance │.│
                ├───┼──────────────────────────────────┼────────────┼───────────────┼────────────────┼─┤
                │[B]│c'est bien à l'avance que         │je détourne │mes regards    │                │.│
                └───┴──────────────────────────────────┴────────────┴───────────────┴────────────────┴─┘
                """.trimIndent()
            testHyperCollation(wA, wB, expectedDot, expectedTable)
        }

        @Disabled
        // both app/rdg and subst/* lead to witness branches
        // join matches from different branches of a witness, if the branches are joined
        // keep a list of which branches are joined (include sub-branches)
        // when sorting the matchlist on witness, deal with matches where multiple joined branches are involved
        // when determining next neighbor, if the next match is in multiple joined branches of the witness, extra neighbors must be generated
        @Test
        @Timeout(15)
        fun trd_642_0() {
            val importer = XMLImporter()
            val wA = importer.importXML(
                    "A", "<e>well <subst><add>at his own pace</add><del>in his own way</del></subst>.</e>")
            val wB = importer.importXML(
                    "B", "<e>well at his own gait.</e>")
            log.info("wA={}", wA.asDot())
            val expectedDot = """
                digraph CollationGraph{
                labelloc=b
                t000 [label="";shape=doublecircle,rank=middle]
                t001 [label="";shape=doublecircle,rank=middle]
                t002 [label=<A,B: well&#9251;<br/>A,B: <i>/e</i>>;penwidth=2]
                t003 [label=<A,B: at&#9251;his&#9251;own&#9251;<br/>A: <i>/e/subst/add</i><br/>B: <i>/e</i><br/>>;penwidth=2]
                t004 [label=<A: pace<br/>A: <i>/e/subst/add</i>>]
                t005 [label=<A: in&#9251;his&#9251;own&#9251;way<br/>A: <i>/e/subst/del</i>>]
                t006 [label=<A,B: .<br/>A,B: <i>/e</i>>;penwidth=2]
                t007 [label=<B: gait<br/>B: <i>/e</i>>]
                t000->t002[label="A,B";penwidth=2]
                t002->t003[label="A,B";penwidth=2]
                t002->t005[label="A"]
                t003->t004[label="A"]
                t003->t007[label="B"]
                t004->t006[label="A"]
                t005->t006[label="A"]
                t006->t001[label="A,B";penwidth=2]
                t007->t006[label="B"]
                }
                """.trimIndent()
            val expectedTable = """
                ┌───┬─────┬──────────────────┬────────┬─┐
                │[A]│     │[+]  at  his   own│        │ │
                │   │well │[-] in his own way│[+] pace│.│
                ├───┼─────┼──────────────────┼────────┼─┤
                │[B]│well │at his own        │gait    │.│
                └───┴─────┴──────────────────┴────────┴─┘
                """.trimIndent()
            testHyperCollation(wA, wB, expectedDot, expectedTable)
        }

        //        1: Duplicates in add en del branches
        //        W1: <root> a long <subst><add> text that is very long </add><del> text that is different</del></subst> </root>
        //        W2: <root> a long text that is very long </root>
        //        Gewenste uitkomst: Moet alleen de add volgen.
        @Disabled
        @Test
        @Timeout(15)
        fun trd_642_1a() {
            val importer = XMLImporter()
            val w1 = importer.importXML(
                    "1", "<root>a long <subst><add>text that is very long</add><del>text that is different</del></subst></root>")
            val w2 = importer.importXML(
                    "2", "<root>a long text that is very long</root>")
            val expectedDot = """
                digraph CollationGraph{
                labelloc=b
                t000 [label="";shape=doublecircle,rank=middle]
                t001 [label="";shape=doublecircle,rank=middle]
                t002 [label=<1,2: a&#9251;long&#9251;<br/>1,2: <i>/root</i>>;penwidth=2]
                t003 [label=<1,2: text&#9251;that&#9251;is&#9251;very&#9251;long<br/>1: <i>/root/subst/add</i><br/>2: <i>/root</i><br/>>;penwidth=2]
                t004 [label=<1: text&#9251;that&#9251;is&#9251;different<br/>1: <i>/root/subst/del</i>>]
                t000->t002[label="1,2";penwidth=2]
                t002->t003[label="1,2";penwidth=2]
                t002->t004[label="1"]
                t003->t001[label="2"]
                t004->t001[label="1"]
                }
                """.trimIndent()
            val expectedTable = """
                ┌───┬───────┬──────────────────────────┐
                │[1]│       │[+] text that is very long│
                │   │a long │[-] text that is different│
                ├───┼───────┼──────────────────────────┤
                │[2]│a long │text that is very long    │
                └───┴───────┴──────────────────────────┘
                """.trimIndent()
            testHyperCollation(w1, w2, expectedDot, expectedTable)
        }

        @Disabled
        @Test
        @Timeout(15)
        fun trd_642_1b() {
            val importer = XMLImporter()
            val w1 = importer.importXML(
                    "1", "<root>a long <subst><del>text that is different</del><add>text that is very long</add></subst></root>")
            val w2 = importer.importXML(
                    "2", "<root>a long text that is very long</root>")
            val expectedDot = """
                digraph CollationGraph{
                labelloc=b
                t000 [label="";shape=doublecircle,rank=middle]
                t001 [label="";shape=doublecircle,rank=middle]
                t002 [label=<1,2: a&#9251;long&#9251;<br/>1,2: <i>/root</i>>;penwidth=2]
                t003 [label=<1: text&#9251;that&#9251;is&#9251;different<br/>1: <i>/root/subst/del</i>>]
                t004 [label=<1,2: text&#9251;that&#9251;is&#9251;very&#9251;long<br/>1: <i>/root/subst/add</i><br/>2: <i>/root</i><br/>>;penwidth=2]
                t000->t002[label="1,2";penwidth=2]
                t002->t003[label="1"]
                t002->t004[label="1,2";penwidth=2]
                t004->t001[label="1,2";penwidth=2]
                }
                """.trimIndent()
            val expectedTable = """
                ┌───┬───────┬──────────────────────────┐
                │[1]│       │[+] text that is very long│
                │   │a long │[-] text that is different│
                ├───┼───────┼──────────────────────────┤
                │[2]│a long │text that is very long    │
                └───┴───────┴──────────────────────────┘
                """.trimIndent()
            testHyperCollation(w1, w2, expectedDot, expectedTable)
        }

        //        2: Add en del branches hebben beide interessante tokens, er zijn geen duplicates.
        //        W1: <root>  a b <subst><add> c d e </add><del> f g </del></subst> </root>
        //        W2: <root> a b c d e f g </root>
        //        Gewenste uitkomst:
        //        root volgen, daarna add, daarna del. Je switcht dan niet heen en weer van branches tijdens de alignment.
//        @Disabled
        @Test
        @Timeout(15)
        fun trd_642_2() {
            val importer = XMLImporter()
            val w1 = importer.importXML(
                    "1", "<root>Cookie Monster <subst><add>wants chocolate cookies</add><del>eating carrots</del></subst></root>")
            val w2 = importer.importXML(
                    "2", "<root>Cookie Monster wants chocolate cookies eating carrots</root>")
            val expectedDot = """
                digraph CollationGraph{
                labelloc=b
                t000 [label="";shape=doublecircle,rank=middle]
                t001 [label=<1,2: Cookie&#9251;Monster&#9251;<br/>1,2: <i>/root</i>>;penwidth=2]
                t002 [label="";shape=doublecircle,rank=middle]
                t003 [label=<1<sup>+</sup>: wants&#9251;chocolate&#9251;cookies<br/>2: wants&#9251;chocolate&#9251;cookies&#9251;<br/>1: <i>/root/subst/add</i><br/>2: <i>/root</i><br/>>;penwidth=2]
                t004 [label=<1<sup>-</sup>,2: eating&#9251;carrots<br/>1: <i>/root/subst/del</i><br/>2: <i>/root</i><br/>>;penwidth=2]
                t000->t001[label=<1,2>;penwidth=2]
                t001->t003[label=<1<sup>+</sup>,2>;penwidth=2]
                t001->t004[label=<1<sup>-</sup>>]
                t003->t002[label=<1<sup>+</sup>>]
                t003->t004[label=<2>]
                t004->t002[label=<1<sup>-</sup>,2>;penwidth=2]
                }
                """.trimIndent()
            val expectedTable = """
                ┌───┬───────────────┬───────────────────────────┬──────────────────┐
                │[1]│Cookie Monster │[+] wants chocolate cookies│[-] eating carrots│
                ├───┼───────────────┼───────────────────────────┼──────────────────┤
                │[2]│Cookie Monster │wants chocolate cookies    │eating carrots    │
                └───┴───────────────┴───────────────────────────┴──────────────────┘
                """.trimIndent()
            testHyperCollation(w1, w2, expectedDot, expectedTable)
        }

        //        3: Branches in branches
        //        W1: <root>  a b <subst><add> c d e <subst><add> f g h</add><del> bla die bla</del></subst></add><del> f g </del></subst> </root>
        //        W2: <root> a b c d e f g h </root>
        //        Gewenste uitkomst:
        //        root volgen, dan de eerste subst/add, dan de tweede subs/add.
//        @Disabled
        @Test
        @Timeout(15)
        fun trd_642_3() {
            val importer = XMLImporter()
            val w1 = importer.importXML(
                    "1", "<root>a b <subst><add>c d e<subst><add>f g h</add><del>bla die bla</del></subst></add><del>f g</del></subst></root>")
            log.info("w1={}", w1.asDot())
            val w2 = importer.importXML(
                    "2", "<root>a b c d e f g h</root>")
            val expectedDot = """
                digraph CollationGraph{
                labelloc=b
                t000 [label="";shape=doublecircle,rank=middle]
                t001 [label=<1,2: a&#9251;b&#9251;<br/>1,2: <i>/root</i>>;penwidth=2]
                t002 [label="";shape=doublecircle,rank=middle]
                t003 [label=<1<sup>+</sup>: c&#9251;d&#9251;e<br/>2: c&#9251;d&#9251;e&#9251;<br/>1: <i>/root/subst/add</i><br/>2: <i>/root</i><br/>>;penwidth=2]
                t004 [label=<1<sup>++</sup>,2: f&#9251;g&#9251;h<br/>1: <i>/root/subst/add/subst/add</i><br/>2: <i>/root</i><br/>>;penwidth=2]
                t005 [label=<1<sup>+-</sup>: bla&#9251;die&#9251;bla<br/>1: <i>/root/subst/add/subst/del</i>>]
                t006 [label=<1<sup>-</sup>: f&#9251;g<br/>1: <i>/root/subst/del</i>>]
                t000->t001[label=<1,2>;penwidth=2]
                t001->t003[label=<1<sup>+</sup>,2>;penwidth=2]
                t001->t006[label=<1<sup>-</sup>>]
                t003->t004[label=<1<sup>++</sup>,2>;penwidth=2]
                t003->t005[label=<1<sup>+-</sup>>]
                t004->t002[label=<2>]
                t004->t006[label=<1<sup>++</sup>>]
                t005->t006[label=<1<sup>+-</sup>>]
                t006->t002[label=<1<sup>-</sup>>]
                }
                """.trimIndent()
            val expectedTable = """
                ┌───┬────┬─────────┬───────────────┬───────┐
                │[1]│    │         │[-] bla die bla│       │
                │   │a b │[+] c d e│[+] f g h      │[-] f g│
                ├───┼────┼─────────┼───────────────┼───────┤
                │[2]│a b │c d e    │f g h          │       │
                └───┴────┴─────────┴───────────────┴───────┘
                """.trimIndent()
            testHyperCollation(w1, w2, expectedDot, expectedTable)
        }

        //        4. De beste match kan ook deels na de branch liggen.
        //        W1: <root>  a b <subst><add> c d <del> e f  </del></subst> e  f g h i j</root>
        //        W2: <root> a b c d e f g h i j </root>
        //        Gewenste uitkomst:
        //        root volgen, dan de add, daar c d uithalen, dan e f g h i j matchen na de branch, want de match na de branch is langer dan wat er in del staat.
        @Disabled
        @Test
        @Timeout(15)
        fun trd_642_4() {
            val importer = XMLImporter()
            val w1 = importer.importXML(
                    "1", "<root>a b <subst><add>c d</add><del>e f</del></subst> e f g h i j</root>")
            val w2 = importer.importXML(
                    "2", "<root>a b c d e f g h i j</root>")
            val expectedDot = """
                digraph CollationGraph{
                labelloc=b
                t000 [label="";shape=doublecircle,rank=middle]
                t001 [label="";shape=doublecircle,rank=middle]
                t002 [label=<1,2: a&#9251;b&#9251;<br/>1,2: <i>/root</i>>;penwidth=2]
                t003 [label=<1: c&#9251;d<br/>2: c&#9251;d&#9251;<br/>1: <i>/root/subst/add</i><br/>2: <i>/root</i><br/>>;penwidth=2]
                t004 [label=<1: e&#9251;f<br/>1: <i>/root/subst/del</i>>]
                t005 [label=<1: &#9251;<br/>1: <i>/root</i>>]
                t006 [label=<1,2: e&#9251;f&#9251;g&#9251;h&#9251;i&#9251;j<br/>1,2: <i>/root</i>>;penwidth=2]
                t000->t002[label="1,2";penwidth=2]
                t002->t003[label="1,2";penwidth=2]
                t002->t004[label="1"]
                t003->t005[label="1"]
                t003->t006[label="2"]
                t004->t005[label="1"]
                t005->t006[label="1"]
                t006->t001[label="1,2";penwidth=2]
                }
                """.trimIndent()
            val expectedTable = """
                ┌───┬────┬───────┬─┬───────────┐
                │[1]│    │[+] c d│ │           │
                │   │a b │[-] e f│ │e f g h i j│
                ├───┼────┼───────┼─┼───────────┤
                │[2]│a b │c d    │ │e f g h i j│
                └───┴────┴───────┴─┴───────────┘
                """.trimIndent()
            testHyperCollation(w1, w2, expectedDot, expectedTable)
        }

        @Test
        @Timeout(60)
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
                t001 [label=<W1,W2<sup>-</sup>: Ik&#9251;had&#9251;een&#9251;buurvrouw,&#9251;<br/>W1: <i>/seg</i><br/>W2: <i>/seg/del</i><br/>>;penwidth=2]
                t002 [label="";shape=doublecircle,rank=middle]
                t003 [label=<W1,W2: een&#9251;paar&#9251;deuren&#9251;verder,&#9251;en&#9251;<br/>W1,W2: <i>/seg</i>>;penwidth=2]
                t004 [label=<W2<sup>+</sup>: Die&#9251;buurvrouw&#9251;woonde&#9251;<br/>W2: <i>/seg/add</i>>]
                t005 [label=<W1<sup>-</sup>: ze<br/>W1: <i>/seg/del</i>>]
                t006 [label=<W1<sup>+</sup>: het<br/>W2: het&#9251;<br/>W1: <i>/seg/add</i><br/>W2: <i>/seg</i><br/>>;penwidth=2]
                t007 [label=<W1: &#9251;<br/>W1: <i>/seg</i>>]
                t008 [label=<W1,W2: was&#9251;zo'n&#9251;type&#9251;<br/>W1,W2: <i>/seg</i>>;penwidth=2]
                t009 [label=<W1,W2<sup>-</sup>: dat&#9251;naar&#9251;het&#9251;<br/>W1: <i>/seg</i><br/>W2: <i>/seg/del</i><br/>>;penwidth=2]
                t010 [label=<W1: muse<br/>W1: <i>/seg</i>>]
                t011 [label=<W1<sup>-</sup>: im<br/>W1: <i>/seg/del</i>>]
                t012 [label=<W2<sup>-</sup>: museum&#9251;<br/>W2: <i>/seg/del</i>>]
                t013 [label=<W1: um&#9251;<br/>W1: <i>/seg</i>>]
                t014 [label=<W1,W2<sup>-</sup>: ging&#9251;en&#9251;<br/>W1: <i>/seg</i><br/>W2: <i>/seg/del</i><br/>>;penwidth=2]
                t015 [label=<W2<sup>-</sup>: cappuccino's&#9251;<br/>W2: <i>/seg/del</i>>]
                t016 [label=<W1: cappuc<br/>W1: <i>/seg</i>>]
                t017 [label=<W1<sup>+</sup>: c<br/>W1: <i>/seg/add</i>>]
                t018 [label=<W1: i<br/>W1: <i>/seg</i>>]
                t019 [label=<W1<sup>-</sup>: o<br/>W1: <i>/seg/del</i>>]
                t020 [label=<W1: no's&#9251;<br/>W1: <i>/seg</i>>]
                t021 [label=<W1,W2<sup>-</sup>: dronk<br/>W1: <i>/seg</i><br/>W2: <i>/seg/del</i><br/>>;penwidth=2]
                t022 [label=<W1<sup>-</sup>: l<br/>W1: <i>/seg/del</i>>]
                t023 [label=<W2<sup>+</sup>: waar&#9251;ik<br/>W2: <i>/seg/add</i>>]
                t024 [label=<W1: ,&#9251;dus&#9251;ik<br/>W2<sup>-</sup>: ,&#9251;dus&#9251;ik&#9251;<br/>W1: <i>/seg</i><br/>W2: <i>/seg/del</i><br/>>;penwidth=2]
                t025 [label=<W2: &#9251;<br/>W2: <i>/seg</i>>]
                t026 [label=<W1<sup>-</sup>: i&#9251;k<br/>W1: <i>/seg/del</i>>]
                t027 [label=<W2<sup>+</sup>: &#9251;ko<br/>W2: <i>/seg/add</i>>]
                t028 [label=<W1: kon&#9251;er&#9251;<br/>W2<sup>-</sup>: kon&#9251;er<br/>W1: <i>/seg</i><br/>W2: <i>/seg/del</i><br/>>;penwidth=2]
                t029 [label=<W2<sup>+-</sup>: m<br/>W2: <i>/seg/add/del</i>>]
                t030 [label=<W1,W2: weinig&#9251;mee<br/>W1,W2: <i>/seg</i>>;penwidth=2]
                t031 [label=<W2<sup>+</sup>: n<br/>W2: <i>/seg/add</i>>]
                t032 [label=<W1,W2: ,&#9251;en&#9251;zij&#9251;kon&#9251;weinig&#9251;<br/>W1,W2: <i>/seg</i>>;penwidth=2]
                t033 [label=<W2: met&#9251;<br/>W2: <i>/seg</i>>]
                t034 [label=<W1: m<br/>W1: <i>/seg</i>>]
                t035 [label=<W1<sup>-</sup>: netr<br/>W1: <i>/seg/del</i>>]
                t036 [label=<W1: et&#9251;<br/>W1: <i>/seg</i>>]
                t037 [label=<W1,W2: mij<br/>W1,W2: <i>/seg</i>>;penwidth=2]
                t038 [label=<W1<sup>-</sup>: ,<br/>W1: <i>/seg/del</i>>]
                t039 [label=<W1<sup>+</sup>: ;<br/>W2: ;&#9251;<br/>W1: <i>/seg/add</i><br/>W2: <i>/seg</i><br/>>;penwidth=2]
                t040 [label=<W1: &#9251;<br/>W1: <i>/seg</i>>]
                t041 [label=<W1,W2: we&#9251;<br/>W1,W2: <i>/seg</i>>;penwidth=2]
                t042 [label=<W1<sup>-</sup>: lk<br/>W1: <i>/seg/del</i>>]
                t043 [label=<W1: &#9251;<br/>W1: <i>/seg</i>>]
                t044 [label=<W1,W2: knikten&#9251;alleen&#9251;naar&#9251;elkaar,&#9251;en&#9251;als&#9251;ik&#9251;<br/>W1,W2: <i>/seg</i>>;penwidth=2]
                t045 [label=<W2: Rocky&#9251;<br/>W2: <i>/seg</i>>]
                t046 [label=<W1: Rock<br/>W1: <i>/seg</i>>]
                t047 [label=<W1<sup>-</sup>: u<br/>W1: <i>/seg/del</i>>]
                t048 [label=<W1: y&#9251;<br/>W1: <i>/seg</i>>]
                t049 [label=<W1,W2: bij&#9251;me&#9251;had,&#9251;<br/>W1,W2: <i>/seg</i>>;penwidth=2]
                t050 [label=<W1<sup>-</sup>: knikte<br/>W1: <i>/seg/del</i>>]
                t051 [label=<W1,W2: maakte&#9251;ze&#9251;van&#9251;het&#9251;knikken&#9251;iets&#9251;dat&#9251;nog&#9251;wat&#9251;sneller&#9251;<br/>W1,W2: <i>/seg</i>>;penwidth=2]
                t052 [label=<W2: afgehandeld&#9251;<br/>W2: <i>/seg</i>>]
                t053 [label=<W1: a<br/>W1: <i>/seg</i>>]
                t054 [label=<W1<sup>-</sup>: g<br/>W1: <i>/seg/del</i>>]
                t055 [label=<W1: fgehandeld&#9251;<br/>W1: <i>/seg</i>>]
                t056 [label=<W1: moest<br/>W2: moest&#9251;<br/>W1,W2: <i>/seg</i>>;penwidth=2]
                t057 [label=<W1<sup>-</sup>: r<br/>W1: <i>/seg/del</i>>]
                t058 [label=<W1: &#9251;<br/>W1: <i>/seg</i>>]
                t059 [label=<W1,W2: worden&#9251;dan&#9251;anders.<br/>W1,W2: <i>/seg</i>>;penwidth=2]
                t000->t001[label=<W1,W2<sup>-</sup>>;penwidth=2]
                t000->t004[label=<W2<sup>+</sup>>]
                t001->t003[label=<W1,W2<sup>-</sup>>;penwidth=2]
                t003->t005[label=<W1<sup>-</sup>>]
                t003->t006[label=<W1<sup>+</sup>,W2>;penwidth=2]
                t004->t003[label=<W2<sup>+</sup>>]
                t005->t007[label=<W1<sup>-</sup>>]
                t006->t007[label=<W1<sup>+</sup>>]
                t006->t008[label=<W2>]
                t007->t008[label=<W1>]
                t008->t009[label=<W1,W2<sup>-</sup>>;penwidth=2]
                t008->t023[label=<W2<sup>+</sup>>]
                t009->t010[label=<W1>]
                t009->t012[label=<W2<sup>-</sup>>]
                t010->t011[label=<W1<sup>-</sup>>]
                t010->t013[label=<W1>]
                t011->t013[label=<W1<sup>-</sup>>]
                t012->t014[label=<W2<sup>-</sup>>]
                t013->t014[label=<W1>]
                t014->t015[label=<W2<sup>-</sup>>]
                t014->t016[label=<W1>]
                t015->t021[label=<W2<sup>-</sup>>]
                t016->t017[label=<W1<sup>+</sup>>]
                t016->t018[label=<W1>]
                t017->t018[label=<W1<sup>+</sup>>]
                t018->t019[label=<W1<sup>-</sup>>]
                t018->t020[label=<W1>]
                t019->t020[label=<W1<sup>-</sup>>]
                t020->t021[label=<W1>]
                t021->t022[label=<W1<sup>-</sup>>]
                t021->t024[label=<W1,W2<sup>-</sup>>;penwidth=2]
                t022->t024[label=<W1<sup>-</sup>>]
                t023->t025[label=<W2<sup>+</sup>>]
                t024->t026[label=<W1<sup>-</sup>>]
                t024->t028[label=<W1,W2<sup>-</sup>>;penwidth=2]
                t025->t030[label=<W2>]
                t026->t028[label=<W1<sup>-</sup>>]
                t027->t029[label=<W2<sup>+-</sup>>]
                t027->t031[label=<W2<sup>+</sup>>]
                t028->t025[label=<W2<sup>-</sup>>]
                t028->t030[label=<W1>]
                t029->t031[label=<W2<sup>+-</sup>>]
                t030->t027[label=<W2<sup>+</sup>>]
                t030->t032[label=<W1,W2>;penwidth=2]
                t031->t032[label=<W2<sup>+</sup>>]
                t032->t033[label=<W2>]
                t032->t034[label=<W1>]
                t033->t037[label=<W2>]
                t034->t035[label=<W1<sup>-</sup>>]
                t034->t036[label=<W1>]
                t035->t036[label=<W1<sup>-</sup>>]
                t036->t037[label=<W1>]
                t037->t038[label=<W1<sup>-</sup>>]
                t037->t039[label=<W1<sup>+</sup>,W2>;penwidth=2]
                t038->t040[label=<W1<sup>-</sup>>]
                t039->t040[label=<W1<sup>+</sup>>]
                t039->t041[label=<W2>]
                t040->t041[label=<W1>]
                t041->t042[label=<W1<sup>-</sup>>]
                t041->t043[label=<W1>]
                t041->t044[label=<W2>]
                t042->t043[label=<W1<sup>-</sup>>]
                t043->t044[label=<W1>]
                t044->t045[label=<W2>]
                t044->t046[label=<W1>]
                t045->t049[label=<W2>]
                t046->t047[label=<W1<sup>-</sup>>]
                t046->t048[label=<W1>]
                t047->t048[label=<W1<sup>-</sup>>]
                t048->t049[label=<W1>]
                t049->t050[label=<W1<sup>-</sup>>]
                t049->t051[label=<W1,W2>;penwidth=2]
                t050->t051[label=<W1<sup>-</sup>>]
                t051->t052[label=<W2>]
                t051->t053[label=<W1>]
                t052->t056[label=<W2>]
                t053->t054[label=<W1<sup>-</sup>>]
                t053->t055[label=<W1>]
                t054->t055[label=<W1<sup>-</sup>>]
                t055->t056[label=<W1>]
                t056->t057[label=<W1<sup>-</sup>>]
                t056->t058[label=<W1>]
                t056->t059[label=<W2>]
                t057->t058[label=<W1<sup>-</sup>>]
                t058->t059[label=<W1>]
                t059->t002[label=<W1,W2>;penwidth=2]
                }
                """.trimIndent()
            val expectedTable = """
                ┌────┬──────────────────────────┬───────────────────────────┬───────┬─┬──────────────┬─────────────────┬───────────┬──────┬───┬────────────┬─────────────────┬─────┬─┬─────┬─────┬─────────┬─────┬─────────────┬───────┬──────────┬─┬──────────┬───────┬─────┬─────┬────────────────────┬────┬────────┬───┬───┬─────┬─┬───┬──────┬─┬──────────────────────────────────────┬──────┬─────┬──┬────────────┬──────────┬───────────────────────────────────────────────────┬────────────┬─────┬───────────┬──────┬─────┬─┬──────────────────┐
                │[W1]│                          │                           │[+] het│ │              │                 │           │      │   │            │                 │     │ │     │     │         │     │             │       │          │ │          │       │     │     │                    │    │        │   │   │[+] ;│ │   │      │ │                                      │      │     │  │            │          │                                                   │            │     │           │      │     │ │                  │
                │    │Ik had een buurvrouw,     │een paar deuren verder, en │[-] ze │ │was zo'n type │dat naar het     │muse       │[-] im│um │ging en     │cappuc           │[+] c│i│[-] o│no's │dronk    │[-] l│, dus ik     │[-] i k│kon er    │ │weinig mee│       │     │     │, en zij kon weinig │m   │[-] netr│et │mij│[-] ,│ │we │[-] lk│ │knikten alleen naar elkaar, en als ik │Rock  │[-] u│y │bij me had, │[-] knikte│maakte ze van het knikken iets dat nog wat sneller │a           │[-] g│fgehandeld │moest │[-] r│ │worden dan anders.│
                ├────┼──────────────────────────┼───────────────────────────┼───────┼─┼──────────────┼─────────────────┼───────────┼──────┼───┼────────────┼─────────────────┼─────┼─┼─────┼─────┼─────────┼─────┼─────────────┼───────┼──────────┼─┼──────────┼───────┼─────┼─────┼────────────────────┼────┼────────┼───┼───┼─────┼─┼───┼──────┼─┼──────────────────────────────────────┼──────┼─────┼──┼────────────┼──────────┼───────────────────────────────────────────────────┼────────────┼─────┼───────────┼──────┼─────┼─┼──────────────────┤
                │[W2]│[+] Die buurvrouw woonde  │                           │       │ │              │[+] waar ik      │           │      │   │            │                 │     │ │     │     │         │     │             │       │          │ │          │       │     │     │                    │    │        │   │   │     │ │   │      │ │                                      │      │     │  │            │          │                                                   │            │     │           │      │     │ │                  │
                │    │[-] Ik had een buurvrouw, │een paar deuren verder, en │het    │ │was zo'n type │[-] dat naar het │[-] museum │      │   │[-] ging en │[-] cappuccino's │     │ │     │     │[-] dronk│     │[-] , dus ik │       │[-] kon er│ │weinig mee│[+] ko │[-] m│[+] n│, en zij kon weinig │met │        │   │mij│;    │ │we │      │ │knikten alleen naar elkaar, en als ik │Rocky │     │  │bij me had, │          │maakte ze van het knikken iets dat nog wat sneller │afgehandeld │     │           │moest │     │ │worden dan anders.│
                └────┴──────────────────────────┴───────────────────────────┴───────┴─┴──────────────┴─────────────────┴───────────┴──────┴───┴────────────┴─────────────────┴─────┴─┴─────┴─────┴─────────┴─────┴─────────────┴───────┴──────────┴─┴──────────┴───────┴─────┴─────┴────────────────────┴────┴────────┴───┴───┴─────┴─┴───┴──────┴─┴──────────────────────────────────────┴──────┴─────┴──┴────────────┴──────────┴───────────────────────────────────────────────────┴────────────┴─────┴───────────┴──────┴─────┴─┴──────────────────┘
                """.trimIndent()
            log.info("w1={}", xml1)
            log.info("w2={}", xml2)
            testHyperCollation(w1, w2, expectedDot, expectedTable)
        }

        @Test
        @Timeout(10000)
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
            log.info(fXML)
            log.info(qXML)
            val expectedDotF = """
                digraph VariantWitnessGraph{
                graph [rankdir=LR]
                labelloc=b
                begin [label="";shape=doublecircle,rank=middle]
                vF_000 [label=<Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/><i>F: /text/s</i>>]
                vF_006 [label=<<br/><i>F: /text/s/lb</i>>]
                vF_007 [label=<werven&#9251;om<br/><i>F: /text/s/del</i>>]
                vF_009 [label=<trachten&#9251;naar<br/><i>F: /text/s/add</i>>]
                vF_011 [label=<&#9251;een&#9251;vrouw,&#x21A9;<br/>&#9251;de&#9251;ongewisheid&#9251;vóór&#9251;de&#9251;<br/><i>F: /text/s</i>>]
                vF_019 [label=<<br/><i>F: /text/s/lb</i>>]
                vF_020 [label=<liefelijke&#9251;toestemming!<br/><i>F: /text/s</i>>]
                end [label="";shape=doublecircle,rank=middle]
                begin->vF_000
                vF_000->vF_006
                vF_006->vF_007
                vF_006->vF_009
                vF_007->vF_011
                vF_009->vF_011
                vF_011->vF_019
                vF_019->vF_020
                vF_020->end
                }
                """.trimIndent()
            verifyDotExport(wF, expectedDotF)
            val expectedDotQ = """
                digraph VariantWitnessGraph{
                graph [rankdir=LR]
                labelloc=b
                begin [label="";shape=doublecircle,rank=middle]
                vQ_000 [label=<Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/><i>Q: /text/s</i>>]
                vQ_006 [label=<werven&#9251;om<br/><i>Q: /text/s/del</i>>]
                vQ_008 [label=<trachten&#9251;naar<br/><i>Q: /text/s/add</i>>]
                vQ_010 [label=<&#9251;een&#9251;<br/><i>Q: /text/s</i>>]
                vQ_012 [label=<<br/><i>Q: /text/s/lb</i>>]
                vQ_013 [label=<vrouw&#9251;!&#x21A9;<br/>&#9251;Die&#9251;dagen&#9251;van&#9251;nerveuze&#9251;verwachting&#9251;vóór&#9251;de&#9251;liefelijke&#9251;toestemming.<br/><i>Q: /text/s</i>>]
                end [label="";shape=doublecircle,rank=middle]
                begin->vQ_000
                vQ_000->vQ_006
                vQ_000->vQ_008
                vQ_006->vQ_010
                vQ_008->vQ_010
                vQ_010->vQ_012
                vQ_012->vQ_013
                vQ_013->end
                }
                """.trimIndent()
            verifyDotExport(wQ, expectedDotQ)
            val expectedDot = """
                digraph CollationGraph{
                labelloc=b
                t000 [label="";shape=doublecircle,rank=middle]
                t001 [label=<F,Q: Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/>F,Q: <i>/text/s</i>>;penwidth=2]
                t002 [label="";shape=doublecircle,rank=middle]
                t003 [label=<F: <br/>F: <i>/text/s/lb</i>>]
                t004 [label=<F<sup>-</sup>,Q<sup>-</sup>: werven&#9251;om<br/>F,Q: <i>/text/s/del</i>>;penwidth=2]
                t005 [label=<F<sup>+</sup>,Q<sup>+</sup>: trachten&#9251;naar<br/>F,Q: <i>/text/s/add</i>>;penwidth=2]
                t006 [label=<F,Q: &#9251;een&#9251;<br/>F,Q: <i>/text/s</i>>;penwidth=2]
                t007 [label=<Q: <br/>Q: <i>/text/s/lb</i>>]
                t008 [label=<F: vrouw<br/>Q: vrouw&#9251;<br/>F,Q: <i>/text/s</i>>;penwidth=2]
                t009 [label=<F: ,&#x21A9;<br/>&#9251;de&#9251;ongewisheid&#9251;<br/>F: <i>/text/s</i>>]
                t010 [label=<Q: !&#x21A9;<br/>&#9251;Die&#9251;dagen&#9251;van&#9251;nerveuze&#9251;verwachting&#9251;<br/>Q: <i>/text/s</i>>]
                t011 [label=<F,Q: vóór&#9251;de&#9251;<br/>F,Q: <i>/text/s</i>>;penwidth=2]
                t012 [label=<F: <br/>F: <i>/text/s/lb</i>>]
                t013 [label=<F,Q: liefelijke&#9251;toestemming<br/>F,Q: <i>/text/s</i>>;penwidth=2]
                t014 [label=<F: !<br/>F: <i>/text/s</i>>]
                t015 [label=<Q: .<br/>Q: <i>/text/s</i>>]
                t000->t001[label=<F,Q>;penwidth=2]
                t001->t003[label=<F>]
                t001->t004[label=<Q<sup>-</sup>>]
                t001->t005[label=<Q<sup>+</sup>>]
                t003->t004[label=<F<sup>-</sup>>]
                t003->t005[label=<F<sup>+</sup>>]
                t004->t006[label=<F<sup>-</sup>,Q<sup>-</sup>>;penwidth=2]
                t005->t006[label=<F<sup>+</sup>,Q<sup>+</sup>>;penwidth=2]
                t006->t007[label=<Q>]
                t006->t008[label=<F>]
                t007->t008[label=<Q>]
                t008->t009[label=<F>]
                t008->t010[label=<Q>]
                t009->t011[label=<F>]
                t010->t011[label=<Q>]
                t011->t012[label=<F>]
                t011->t013[label=<Q>]
                t012->t013[label=<F>]
                t013->t014[label=<F>]
                t013->t015[label=<Q>]
                t014->t002[label=<F>]
                t015->t002[label=<Q>]
                }
                """.trimIndent()
            val expectedTable = """
                ┌───┬────────────────────────────────┬─────┬─────────────────┬─────┬─────┬──────┬─────────────────────────────────────┬────────┬─────┬──────────────────────┬─┐
                │[F]│                                │     │[+] trachten naar│     │     │      │                                     │        │     │                      │ │
                │   │Hoe zoet moet nochtans zijn dit │<lb/>│[-] werven om    │een  │     │vrouw │, de ongewisheid                     │vóór de │<lb/>│liefelijke toestemming│!│
                ├───┼────────────────────────────────┼─────┼─────────────────┼─────┼─────┼──────┼─────────────────────────────────────┼────────┼─────┼──────────────────────┼─┤
                │[Q]│                                │     │[+] trachten naar│     │     │      │                                     │        │     │                      │ │
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

        @Test
        @Timeout(10000)
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
                vF_000 [label=<De&#9251;vent&#9251;was&#9251;woedend&#9251;en&#9251;maakte&#9251;<br/><i>F: /text/s</i>>]
                vF_006 [label=<Shiriar<br/><i>F: /text/s/del</i>>]
                vF_007 [label=<&#9251;den&#9251;bedremmelden&#x21A9;<br/>&#9251;Sultan&#9251;uit&#9251;voor&#9251;"lompen&#9251;boer".<br/><i>F: /text/s</i>>]
                end [label="";shape=doublecircle,rank=middle]
                begin->vF_000
                vF_000->vF_006
                vF_000->vF_007
                vF_006->vF_007
                vF_007->end
                }
                """.trimIndent()
            verifyDotExport(wF, expectedDotF)
            val expectedDotQ = """
                digraph VariantWitnessGraph{
                graph [rankdir=LR]
                labelloc=b
                begin [label="";shape=doublecircle,rank=middle]
                vQ_000 [label=<De&#9251;vent&#9251;was&#9251;woedend&#9251;en&#9251;maakte&#9251;<br/><i>Q: /text/s</i>>]
                vQ_006 [label=<Shiriar<br/><i>Q: /text/s/del</i>>]
                vQ_007 [label=<den&#9251;bedremmelden&#9251;<br/><i>Q: /text/s/add</i>>]
                vQ_011 [label=<&#9251;uit&#9251;voor&#9251;"lompen&#9251;boer".<br/><i>Q: /text/s</i>>]
                vQ_009 [label=<man<br/><i>Q: /text/s/add/del</i>>]
                vQ_010 [label=<Sultan<br/><i>Q: /text/s/add/add</i>>]
                end [label="";shape=doublecircle,rank=middle]
                begin->vQ_000
                vQ_000->vQ_006
                vQ_000->vQ_007
                vQ_006->vQ_011
                vQ_007->vQ_009
                vQ_007->vQ_010
                vQ_009->vQ_011
                vQ_010->vQ_011
                vQ_011->end
                }
                """.trimIndent()
            verifyDotExport(wQ, expectedDotQ)
            val expectedDot = """
                digraph CollationGraph{
                labelloc=b
                t000 [label="";shape=doublecircle,rank=middle]
                t001 [label=<F,Q: De&#9251;vent&#9251;was&#9251;woedend&#9251;en&#9251;maakte&#9251;<br/>F,Q: <i>/text/s</i>>;penwidth=2]
                t002 [label="";shape=doublecircle,rank=middle]
                t003 [label=<F<sup>-</sup>,Q<sup>-</sup>: Shiriar<br/>F,Q: <i>/text/s/del</i>>;penwidth=2]
                t004 [label=<F: &#9251;<br/>F: <i>/text/s</i>>]
                t005 [label=<F: den&#9251;bedremmelden&#x21A9;<br/>&#9251;<br/>Q<sup>+</sup>: den&#9251;bedremmelden&#9251;<br/>F: <i>/text/s</i><br/>Q: <i>/text/s/add</i><br/>>;penwidth=2]
                t006 [label=<Q<sup>+-</sup>: man<br/>Q: <i>/text/s/add/del</i>>]
                t007 [label=<F: Sultan&#9251;<br/>Q<sup>++</sup>: Sultan<br/>F: <i>/text/s</i><br/>Q: <i>/text/s/add/add</i><br/>>;penwidth=2]
                t008 [label=<F,Q: uit&#9251;voor&#9251;"lompen&#9251;boer".<br/>F,Q: <i>/text/s</i>>;penwidth=2]
                t009 [label=<Q: &#9251;<br/>Q: <i>/text/s</i>>]
                t000->t001[label=<F,Q>;penwidth=2]
                t001->t003[label=<F<sup>-</sup>,Q<sup>-</sup>>;penwidth=2]
                t001->t004[label=<F>]
                t001->t005[label=<Q<sup>+</sup>>]
                t003->t004[label=<F<sup>-</sup>>]
                t003->t009[label=<Q<sup>-</sup>>]
                t004->t005[label=<F>]
                t005->t006[label=<Q<sup>+-</sup>>]
                t005->t007[label=<F,Q<sup>++</sup>>;penwidth=2]
                t006->t009[label=<Q<sup>+-</sup>>]
                t007->t008[label=<F>]
                t007->t009[label=<Q<sup>++</sup>>]
                t008->t002[label=<F,Q>;penwidth=2]
                t009->t008[label=<Q>]
                }
                """.trimIndent()
            val expectedTable = """
                ┌───┬──────────────────────────────┬───────────┬─┬─────────────────────┬──────────┬─┬───────────────────────┐
                │[F]│De vent was woedend en maakte │[-] Shiriar│ │den bedremmelden     │Sultan    │ │uit voor "lompen boer".│
                ├───┼──────────────────────────────┼───────────┼─┼─────────────────────┼──────────┼─┼───────────────────────┤
                │[Q]│                              │           │ │                     │[+] Sultan│ │                       │
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
                t001 [label=<A,B: The&#9251;dog's&#9251;<br/>A,B: <i>/text</i>>;penwidth=2]
                t002 [label="";shape=doublecircle,rank=middle]
                t003 [label=<A,B<sup>-</sup>: big&#9251;<br/>A: <i>/text</i><br/>B: <i>/text/del</i><br/>>;penwidth=2]
                t004 [label=<B<sup>-</sup>: black&#9251;ears<br/>B: <i>/text/del</i>>]
                t005 [label=<A,B<sup>+</sup>: eyes<br/>A: <i>/text</i><br/>B: <i>/text/add</i><br/>>;penwidth=2]
                t006 [label=<A,B: .<br/>A,B: <i>/text</i>>;penwidth=2]
                t007 [label=<B<sup>+</sup>: brown&#9251;<br/>B: <i>/text/add</i>>]
                t000->t001[label=<A,B>;penwidth=2]
                t001->t003[label=<A,B<sup>-</sup>>;penwidth=2]
                t001->t007[label=<B<sup>+</sup>>]
                t003->t004[label=<B<sup>-</sup>>]
                t003->t005[label=<A>]
                t004->t006[label=<B<sup>-</sup>>]
                t005->t006[label=<A,B<sup>+</sup>>;penwidth=2]
                t006->t002[label=<A,B>;penwidth=2]
                t007->t005[label=<B<sup>+</sup>>]
                }
                """.trimIndent()
            val expectedTable = """
                ┌───┬──────────┬──────────┬──────────────┬─┐
                │[A]│The dog's │big       │eyes          │.│
                ├───┼──────────┼──────────┼──────────────┼─┤
                │[B]│          │[+] brown │[+] eyes      │ │
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

        @Test
        @Timeout(10000)
        fun testTranspositionAndDuplication() {
            val importer = XMLImporter()
            val wF = importer.importXML("A", "<text>T b b b b b b b Y</text>")
            val wQ = importer.importXML("B", "<text>X b b b b b b b T</text>")
            val expectedDot = """
                digraph CollationGraph{
                labelloc=b
                t000 [label="";shape=doublecircle,rank=middle]
                t001 [label=<A: T&#9251;<br/>A: <i>/text</i>>]
                t002 [label="";shape=doublecircle,rank=middle]
                t003 [label=<B: X&#9251;<br/>B: <i>/text</i>>]
                t004 [label=<A,B: b&#9251;b&#9251;b&#9251;b&#9251;b&#9251;b&#9251;b&#9251;<br/>A,B: <i>/text</i>>;penwidth=2]
                t005 [label=<A: Y<br/>A: <i>/text</i>>]
                t006 [label=<B: T<br/>B: <i>/text</i>>]
                t000->t001[label=<A>]
                t000->t003[label=<B>]
                t001->t004[label=<A>]
                t003->t004[label=<B>]
                t004->t005[label=<A>]
                t004->t006[label=<B>]
                t005->t002[label=<A>]
                t006->t002[label=<B>]
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

        @Test
        @Timeout(10000)
        fun testDoubleTransposition() {
            val importer = XMLImporter()
            val wF = importer.importXML("A", "<text>A b C d E C f G H</text>")
            val wQ = importer.importXML("B", "<text>A H i j E C G k</text>")
            val expectedDot = """
                digraph CollationGraph{
                labelloc=b
                t000 [label="";shape=doublecircle,rank=middle]
                t001 [label=<A,B: A&#9251;<br/>A,B: <i>/text</i>>;penwidth=2]
                t002 [label="";shape=doublecircle,rank=middle]
                t003 [label=<A: b&#9251;C&#9251;d&#9251;<br/>A: <i>/text</i>>]
                t004 [label=<B: H&#9251;i&#9251;j&#9251;<br/>B: <i>/text</i>>]
                t005 [label=<A,B: E&#9251;C&#9251;<br/>A,B: <i>/text</i>>;penwidth=2]
                t006 [label=<A: f&#9251;<br/>A: <i>/text</i>>]
                t007 [label=<A,B: G&#9251;<br/>A,B: <i>/text</i>>;penwidth=2]
                t008 [label=<B: k<br/>B: <i>/text</i>>]
                t009 [label=<A: H<br/>A: <i>/text</i>>]
                t000->t001[label=<A,B>;penwidth=2]
                t001->t003[label=<A>]
                t001->t004[label=<B>]
                t003->t005[label=<A>]
                t004->t005[label=<B>]
                t005->t006[label=<A>]
                t005->t007[label=<B>]
                t006->t007[label=<A>]
                t007->t008[label=<B>]
                t007->t009[label=<A>]
                t008->t002[label=<B>]
                t009->t002[label=<A>]
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

        @Test
        @Timeout(10000)
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
            log.info("H: {}", xml1)
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
            log.info("T: {}", xml2)
            val wQ = importer.importXML("T", xml2)
            val expectedDot = """
                digraph CollationGraph{
                labelloc=b
                t000 [label="";shape=doublecircle,rank=middle]
                t001 [label=<H: Leaning&#9251;her&#9251;bony&#9251;breast&#9251;on&#9251;the&#9251;hard&#9251;thorn&#9251;<br/>T<sup>+</sup>: leaning&#9251;her&#9251;bony&#9251;breast&#9251;on&#9251;the&#9251;hard&#9251;thorn<br/>H: <i>/text/div/s</i><br/>T: <i>/text/p/s/add</i><br/>>;penwidth=2]
                t002 [label="";shape=doublecircle,rank=middle]
                t003 [label=<T: &#9251;granting,&#9251;as&#9251;she&#9251;stood&#9251;the&#9251;chair&#9251;straight&#9251;by&#9251;the&#9251;dressing&#9251;table,&#9251;<br/>T: <i>/text/p/s</i>>]
                t004 [label=<H: she&#9251;crooned&#9251;out&#9251;<br/>H: <i>/text/div/s</i>>]
                t005 [label=<H: her&#9251;forgiveness<br/>T: her&#9251;forgiveness&#9251;<br/>H: <i>/text/div/s</i><br/>T: <i>/text/p/s</i><br/>>;penwidth=2]
                t006 [label=<H,T: .Was&#9251;it&#9251;then&#9251;that&#9251;she&#9251;had&#9251;her&#9251;consolations&#9251;<br/>H: <i>/text/div/s</i><br/>T: <i>/text/p/s</i><br/>>;penwidth=2]
                t007 [label=<T: ,&#9251;<br/>T: <i>/text/p/s</i>>]
                t008 [label=<T: of&#9251;it&#9251;all<br/>T: <i>/text/p/s</i>>]
                t009 [label=<T: ...&#9251;<br/>T: <i>/text/p/s</i>>]
                t000->t001[label=<H>]
                t000->t003[label=<T>]
                t001->t004[label=<H>]
                t001->t007[label=<T<sup>+</sup>>]
                t003->t001[label=<T<sup>+</sup>>]
                t003->t007[label=<T>]
                t004->t005[label=<H>]
                t005->t006[label=<H>]
                t005->t008[label=<T>]
                t006->t002[label=<H>]
                t006->t009[label=<T>]
                t007->t005[label=<T>]
                t008->t006[label=<T>]
                t009->t002[label=<T>]
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

        @Test
        @Timeout(10000)
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
            log.info("N: {}", xmlN)
            val xmlF = """
                <text>
                <p>
                <s>so infinitely miserable, so destitute of every hope of consolation to live?</s> <s>Oh, no! ... </s>
                </p></text>
                """.trimIndent()
            log.info("F: {}", xmlF)
            val wQ = importer.importXML("F", xmlF)
            val expectedDot = """
                digraph CollationGraph{
                labelloc=b
                t000 [label="";shape=doublecircle,rank=middle]
                t001 [label=<F,N: so&#9251;<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>;penwidth=2]
                t002 [label="";shape=doublecircle,rank=middle]
                t003 [label=<F: infinitely&#9251;miserable,&#9251;so&#9251;<br/>F: <i>/text/p/s</i>>]
                t004 [label=<F,N: destitute&#9251;of&#9251;every&#9251;hope&#9251;of&#9251;consolation&#9251;to&#9251;live<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>;penwidth=2]
                t005 [label=<N<sup>-</sup>: -<br/>N: <i>/text/s/del</i>>]
                t006 [label=<F,N<sup>+</sup>: ?<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s/add</i><br/>>;penwidth=2]
                t007 [label=<F,N: &#9251;<br/>F: <i>/text/p</i><br/>N: <i>/text/s</i><br/>>;penwidth=2]
                t008 [label=<N: -&#9251;<br/>N: <i>/text/s</i>>]
                t009 [label=<F: Oh<br/>N: oh&#9251;<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>;penwidth=2]
                t010 [label=<F: ,&#9251;<br/>F: <i>/text/p/s</i>>]
                t011 [label=<F: no<br/>N: no&#9251;<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>;penwidth=2]
                t012 [label=<F: !&#9251;<br/>F: <i>/text/p/s</i>>]
                t013 [label=<F: ...&#9251;<br/>N: ...&#x21A9;<br/><br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>;penwidth=2]
                t000->t001[label=<F,N>;penwidth=2]
                t001->t003[label=<F>]
                t001->t004[label=<N>]
                t003->t004[label=<F>]
                t004->t005[label=<N<sup>-</sup>>]
                t004->t006[label=<F,N<sup>+</sup>>;penwidth=2]
                t005->t007[label=<N<sup>-</sup>>]
                t006->t007[label=<F,N<sup>+</sup>>;penwidth=2]
                t007->t009[label=<F,N>;penwidth=2]
                t008->t013[label=<N>]
                t009->t010[label=<F>]
                t009->t011[label=<N>]
                t010->t011[label=<F>]
                t011->t008[label=<N>]
                t011->t012[label=<F>]
                t012->t013[label=<F>]
                t013->t002[label=<F,N>;penwidth=2]
                }
                """.trimIndent()
//        val expected1 = """
//            digraph CollationGraph{
//            labelloc=b
//            t000 [label="";shape=doublecircle,rank=middle]
//            t001 [label="";shape=doublecircle,rank=middle]
//            t002 [label=<F: so&#9251;infinitely&#9251;miserable,&#9251;<br/>F: <i>/text/p/s</i>>]
//            t003 [label=<F,N: ?<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s/add</i><br/>>]
//            t004 [label=<F,N: &#9251;<br/>F: <i>/text/p</i><br/>N: <i>/text/s</i><br/>>]
//            t005 [label=<F: Oh<br/>N: oh&#9251;<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>]
//            t006 [label=<F: ,&#9251;<br/>F: <i>/text/p/s</i>>]
//            t007 [label=<F: no<br/>N: no&#9251;<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>]
//            t008 [label=<F: !&#9251;<br/>F: <i>/text/p/s</i>>]
//            t009 [label=<F: ...&#9251;<br/>N: ...&#x21A9;<br/><br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>]
//            t010 [label=<F,N: so&#9251;destitute&#9251;of&#9251;every&#9251;hope&#9251;of&#9251;consolation&#9251;to&#9251;live<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>]
//            t011 [label=<N: -&#9251;<br/>N: <i>/text/s</i>>]
//            t012 [label=<N: -<br/>N: <i>/text/s/del</i>>]
//            t000->t002[label="F"]
//            t000->t010[label="N"]
//            t002->t010[label="F"]
//            t003->t004[label="F,N"]
//            t004->t005[label="F,N"]
//            t005->t006[label="F"]
//            t005->t007[label="N"]
//            t006->t007[label="F"]
//            t007->t008[label="F"]
//            t007->t011[label="N"]
//            t008->t009[label="F"]
//            t009->t001[label="F,N"]
//            t010->t003[label="F,N"]
//            t010->t012[label="N"]
//            t011->t009[label="N"]
//            t012->t004[label="N"]
//            }
//            """.trimIndent()
            val expectedTable = """
                ┌───┬───┬─────────────────────────┬──────────────────────────────────────────────┬─────┬─┬───┬──┬───┬──┬────┐
                │[F]│so │infinitely miserable, so │destitute of every hope of consolation to live│?    │ │Oh │, │no │! │... │
                ├───┼───┼─────────────────────────┼──────────────────────────────────────────────┼─────┼─┼───┼──┼───┼──┼────┤
                │[N]│   │                         │                                              │[+] ?│ │   │  │   │  │    │
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

        @Test
        @Timeout(10000)
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
            log.info("N: {}", xmlN)
            val xmlF = """
                <text>
                <s>Frankenstein discovered
                <del rend="strikethrough">or</del>
                <add place="superlinear">that I</add> made notes concerning his history; he asked to see them and then himself corrected and augmented them in many places
                </s>
                </text>
                """.trimIndent()
            log.info("F: {}", xmlF)
            val wQ = importer.importXML("F", xmlF)
            val expectedDot = """
                digraph CollationGraph{
                labelloc=b
                t000 [label="";shape=doublecircle,rank=middle]
                t001 [label=<F: Frankenstein&#9251;discovered<br/>N: Frankenstein&#9251;discovered&#9251;<br/>F,N: <i>/text/s</i>>;penwidth=2]
                t002 [label="";shape=doublecircle,rank=middle]
                t003 [label=<F<sup>-</sup>: or<br/>N: or&#9251;<br/>F: <i>/text/s/del</i><br/>N: <i>/text/s</i><br/>>;penwidth=2]
                t004 [label=<F<sup>+</sup>: that&#9251;I<br/>N: that&#9251;I&#9251;<br/>F: <i>/text/s/add</i><br/>N: <i>/text/s</i><br/>>;penwidth=2]
                t005 [label=<N: detailed&#9251;<br/>N: <i>/text/s</i>>]
                t006 [label=<F: &#9251;<br/>F: <i>/text/s</i>>]
                t007 [label=<F: made&#9251;notes&#9251;concerning&#9251;his&#9251;history<br/>N: made&#9251;notes&#9251;concerning&#9251;his&#9251;history&#9251;<br/>F,N: <i>/text/s</i>>;penwidth=2]
                t008 [label=<F: ;&#9251;<br/>F: <i>/text/s</i>>]
                t009 [label=<F,N: he&#9251;asked&#9251;to&#9251;see&#9251;them&#9251;<br/>F,N: <i>/text/s</i>>;penwidth=2]
                t010 [label=<N: &amp;&#9251;<br/>N: <i>/text/s</i>>]
                t011 [label=<F: and&#9251;then&#9251;<br/>F: <i>/text/s</i>>]
                t012 [label=<F: himself&#9251;corrected&#9251;<br/>N: himself&#9251;corrected&#x21A9;<br/><br/>F,N: <i>/text/s</i>>;penwidth=2]
                t013 [label=<F: and&#9251;augmented&#9251;<br/>N<sup>+</sup>: and&#9251;augmented<br/>F: <i>/text/s</i><br/>N: <i>/text/s/add</i><br/>>;penwidth=2]
                t014 [label=<F: them&#9251;in&#9251;many&#9251;places&#x21A9;<br/><br/>N: them&#9251;in&#9251;many&#9251;places<br/>F,N: <i>/text/s</i>>;penwidth=2]
                t000->t001[label=<F,N>;penwidth=2]
                t001->t003[label=<F<sup>-</sup>>]
                t001->t004[label=<F<sup>+</sup>,N>;penwidth=2]
                t003->t006[label=<F<sup>-</sup>>]
                t003->t007[label=<N>]
                t004->t005[label=<N>]
                t004->t006[label=<F<sup>+</sup>>]
                t005->t003[label=<N>]
                t006->t007[label=<F>]
                t007->t008[label=<F>]
                t007->t009[label=<N>]
                t008->t009[label=<F>]
                t009->t010[label=<N>]
                t009->t011[label=<F>]
                t010->t012[label=<N>]
                t011->t012[label=<F>]
                t012->t013[label=<F,N<sup>+</sup>>;penwidth=2]
                t012->t014[label=<N>]
                t013->t014[label=<F,N<sup>+</sup>>;penwidth=2]
                t014->t002[label=<F,N>;penwidth=2]
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

        @Test
        @Timeout(10000)
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
                t001 [label=<F: Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/>F: <i>/text/s</i>>]
                t002 [label="";shape=doublecircle,rank=middle]
                t003 [label=<F: <br/>F: <i>/text/s/lb</i>>]
                t004 [label=<F<sup>-</sup>: werven&#9251;om<br/>F: <i>/text/s/del</i>>]
                t005 [label=<F<sup>+</sup>: trachten&#9251;naar<br/>F: <i>/text/s/add</i>>]
                t006 [label=<F: &#9251;een&#9251;vrouw,&#x21A9;<br/>&#9251;de&#9251;ongewisheid&#9251;vóór&#9251;de&#9251;<br/>F: <i>/text/s</i>>]
                t007 [label=<F: <br/>F: <i>/text/s/lb</i>>]
                t008 [label=<F: liefelijke&#9251;toestemming!<br/>F: <i>/text/s</i>>]
                t000->t001[label=<F>]
                t001->t003[label=<F>]
                t003->t004[label=<F<sup>-</sup>>]
                t003->t005[label=<F<sup>+</sup>>]
                t004->t006[label=<F<sup>-</sup>>]
                t005->t006[label=<F<sup>+</sup>>]
                t006->t007[label=<F>]
                t007->t008[label=<F>]
                t008->t002[label=<F>]
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
                t001 [label=<F: Hoe&nbsp;zoet&nbsp;moet&nbsp;nochtans&nbsp;zijn&nbsp;dit&nbsp;>]
                t002 [label="";shape=doublecircle,rank=middle]
                t003 [label=<F: >]
                t004 [label=<F<sup>-</sup>: werven&nbsp;om>]
                t005 [label=<F<sup>+</sup>: trachten&nbsp;naar>]
                t006 [label=<F: &nbsp;een&nbsp;vrouw,&#x21A9;<br/>&nbsp;de&nbsp;ongewisheid&nbsp;vóór&nbsp;de&nbsp;>]
                t007 [label=<F: >]
                t008 [label=<F: liefelijke&nbsp;toestemming!>]
                t000->t001[label=<F>]
                t001->t003[label=<F>]
                t003->t004[label=<F<sup>-</sup>>]
                t003->t005[label=<F<sup>+</sup>>]
                t004->t006[label=<F<sup>-</sup>>]
                t005->t006[label=<F<sup>+</sup>>]
                t006->t007[label=<F>]
                t007->t008[label=<F>]
                t008->t002[label=<F>]
                }
                """.trimIndent()
            assertThat(dotWithoutMarkupAndWhitespaceEmphasis).isEqualTo(expected2)

            // System.out.println(dot);
            // writeGraph(dot, "graph");
        }

        @Nested
        inner class AppRdgTest {
            @Test
            @Timeout(10000)
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
                    t001 [label=<W1,W2: One&#9251;must&#9251;have&#9251;lived&#9251;longer&#9251;with&#9251;<br/>W1,W2: <i>/s</i>>;penwidth=2]
                    t002 [label="";shape=doublecircle,rank=middle]
                    t003 [label=<W1: this<br/>W2: this&#9251;<br/>W1: <i>/s/app/rdg</i><br/>W2: <i>/s</i><br/>>;penwidth=2]
                    t004 [label=<W1: such&#9251;a<br/>W1: <i>/s/app/rdg</i>>]
                    t005 [label=<W1: a<br/>W1: <i>/s/app/rdg</i>>]
                    t006 [label=<W1: &#9251;<br/>W1: <i>/s</i>>]
                    t007 [label=<W1,W2: system,&#9251;to&#9251;appreciate&#9251;its&#9251;advantages.<br/>W1,W2: <i>/s</i>>;penwidth=2]
                    t000->t001[label=<W1,W2>;penwidth=2]
                    t001->t003[label=<W1,W2>;penwidth=2]
                    t001->t004[label=<W1>]
                    t001->t005[label=<W1>]
                    t003->t006[label=<W1>]
                    t003->t007[label=<W2>]
                    t004->t006[label=<W1>]
                    t005->t006[label=<W1>]
                    t006->t007[label=<W1>]
                    t007->t002[label=<W1,W2>;penwidth=2]
                    }
                    """.trimIndent()
                val expectedTable = """
                    ┌────┬────────────────────────────────┬──────────┬─┬─────────────────────────────────────┐
                    │[W1]│                                │<3> a     │ │                                     │
                    │    │                                │<2> such a│ │                                     │
                    │    │One must have lived longer with │<1> this  │ │system, to appreciate its advantages.│
                    ├────┼────────────────────────────────┼──────────┼─┼─────────────────────────────────────┤
                    │[W2]│One must have lived longer with │this      │ │system, to appreciate its advantages.│
                    └────┴────────────────────────────────┴──────────┴─┴─────────────────────────────────────┘
                    """.trimIndent()
                testHyperCollation(wF, wQ, expectedDot, expectedTable)
            }

            @Test
            @Timeout(10)
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
                    t001 [label=<W1,W2: One&#9251;must&#9251;have&#9251;lived&#9251;longer&#9251;with&#9251;<br/>W1,W2: <i>/s</i>>;penwidth=2]
                    t002 [label="";shape=doublecircle,rank=middle]
                    t003 [label=<W1<sup>1-</sup>: this<br/>W2: this&#9251;<br/>W1: <i>/s/app/rdg[@varSeq='1']/del</i><br/>W2: <i>/s</i><br/>>;penwidth=2]
                    t004 [label=<W1<sup>2-+</sup>: such&#9251;a<br/>W1: <i>/s/app/rdg[@varSeq='2']/del/add</i>>]
                    t005 [label=<W1<sup>3+</sup>: a<br/>W1: <i>/s/app/rdg[@varSeq='3']/add</i>>]
                    t006 [label=<W1: &#9251;<br/>W1: <i>/s</i>>]
                    t007 [label=<W1,W2: system,&#9251;to&#9251;appreciate&#9251;its&#9251;advantages.<br/>W1,W2: <i>/s</i>>;penwidth=2]
                    t000->t001[label=<W1,W2>;penwidth=2]
                    t001->t003[label=<W1<sup>1-</sup>,W2>;penwidth=2]
                    t001->t004[label=<W1<sup>2-+</sup>>]
                    t001->t005[label=<W1<sup>3+</sup>>]
                    t003->t006[label=<W1<sup>1-</sup>>]
                    t003->t007[label=<W2>]
                    t004->t006[label=<W1<sup>2-+</sup>>]
                    t005->t006[label=<W1<sup>3+</sup>>]
                    t006->t007[label=<W1>]
                    t007->t002[label=<W1,W2>;penwidth=2]
                    }
                    """.trimIndent()
                val expectedTable = """
                    ┌────┬────────────────────────────────┬──────────────┬─┬─────────────────────────────────────┐
                    │[W1]│                                │<3>[+] a      │ │                                     │
                    │    │                                │<2>[-+] such a│ │                                     │
                    │    │One must have lived longer with │<1>[-] this   │ │system, to appreciate its advantages.│
                    ├────┼────────────────────────────────┼──────────────┼─┼─────────────────────────────────────┤
                    │[W2]│One must have lived longer with │this          │ │system, to appreciate its advantages.│
                    └────┴────────────────────────────────┴──────────────┴─┴─────────────────────────────────────┘
                    """.trimIndent()
                testHyperCollation(wF, wQ, expectedDot, expectedTable)
            }

        }

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
            log.info("Collating took {} ms.", duration)
            val collation = CollationGraphNodeJoiner.join(collation0)
            SoftAssertions().apply {
                val dot = CollationGraphVisualizer.toDot(
                        collation,
                        emphasizeWhitespace = true,
                        hideMarkup = false
                )
                log.debug("dot=\n{}", dot)
                writeGraph(dot, "graph")
                assertThat(dot).isEqualTo(expectedDot)

                val table = CollationGraphVisualizer.toTableASCII(collation, false)
                        .replace(" ", " ")
                log.debug("table=\n{}", table)
                assertThat(table).isEqualTo(expectedTable.replace("\n", System.lineSeparator()))

                val html = CollationGraphVisualizer.toTableHTML(collation, false)
                log.debug("html=\n{}", html)
                assertAll()
            }

            return collation
        }
    }

    private fun VariantWitnessGraph.asDot(): String =
            DotFactory(emphasizeWhitespace = true).fromVariantWitnessGraphSimple(this)

    @Nested
    inner class ThreeWitnessTests : HyperCollateTest() {
        @Test
        @Timeout(10000)
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
                t001 [label=<F,Q,Z: Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/>F,Q,Z: <i>/text/s</i>>;penwidth=2]
                t002 [label="";shape=doublecircle,rank=middle]
                t003 [label=<F: <br/>F: <i>/text/s/lb</i>>]
                t004 [label=<F<sup>-</sup>,Q<sup>-</sup>: werven&#9251;om<br/>F,Q: <i>/text/s/del</i>>;penwidth=2]
                t005 [label=<F<sup>+</sup>: trachten&#9251;naar<br/>Q<sup>+</sup>: trachten&#9251;naar<br/>Z: trachten&#9251;naar&#9251;<br/>F: <i>/text/s/add</i><br/>Q: <i>/text/s/add</i><br/>Z: <i>/text/s</i><br/>>;penwidth=2]
                t006 [label=<F,Q: &#9251;<br/>F,Q: <i>/text/s</i>>;penwidth=2]
                t007 [label=<F,Q,Z: een&#9251;<br/>F,Q,Z: <i>/text/s</i>>;penwidth=2]
                t008 [label=<Q: <br/>Q: <i>/text/s/lb</i>>]
                t009 [label=<F: vrouw<br/>Q: vrouw&#9251;<br/>Z: vrouw&#9251;<br/>F,Q,Z: <i>/text/s</i>>;penwidth=2]
                t010 [label=<F: ,&#x21A9;<br/>&#9251;de&#9251;<br/>F: <i>/text/s</i>>]
                t011 [label=<Q: !&#x21A9;<br/>&#9251;Die&#9251;dagen&#9251;van&#9251;<br/>Z: !Die&#9251;dagen&#9251;van&#9251;<br/>Q,Z: <i>/text/s</i>>;penwidth=2]
                t012 [label=<F,Z: ongewisheid&#9251;<br/>F,Z: <i>/text/s</i>>;penwidth=2]
                t013 [label=<F,Q,Z: vóór&#9251;de&#9251;<br/>F,Q,Z: <i>/text/s</i>>;penwidth=2]
                t014 [label=<Q: nerveuze&#9251;verwachting&#9251;<br/>Q: <i>/text/s</i>>]
                t015 [label=<F: <br/>F: <i>/text/s/lb</i>>]
                t016 [label=<F,Q,Z: liefelijke&#9251;toestemming<br/>F,Q,Z: <i>/text/s</i>>;penwidth=2]
                t017 [label=<F: !<br/>F: <i>/text/s</i>>]
                t018 [label=<Q,Z: .<br/>Q,Z: <i>/text/s</i>>;penwidth=2]
                t000->t001[label=<F,Q,Z>;penwidth=2]
                t001->t003[label=<F>]
                t001->t004[label=<Q<sup>-</sup>>]
                t001->t005[label=<Q<sup>+</sup>,Z>;penwidth=2]
                t003->t004[label=<F<sup>-</sup>>]
                t003->t005[label=<F<sup>+</sup>>]
                t004->t006[label=<F<sup>-</sup>,Q<sup>-</sup>>;penwidth=2]
                t005->t006[label=<F<sup>+</sup>,Q<sup>+</sup>>;penwidth=2]
                t005->t007[label=<Z>]
                t006->t007[label=<F,Q>;penwidth=2]
                t007->t008[label=<Q>]
                t007->t009[label=<F,Z>;penwidth=2]
                t008->t009[label=<Q>]
                t009->t010[label=<F>]
                t009->t011[label=<Q,Z>;penwidth=2]
                t010->t012[label=<F>]
                t011->t012[label=<Z>]
                t011->t014[label=<Q>]
                t012->t013[label=<F,Z>;penwidth=2]
                t013->t015[label=<F>]
                t013->t016[label=<Q,Z>;penwidth=2]
                t014->t013[label=<Q>]
                t015->t016[label=<F>]
                t016->t017[label=<F>]
                t016->t018[label=<Q,Z>;penwidth=2]
                t017->t002[label=<F>]
                t018->t002[label=<Q,Z>;penwidth=2]
                }
                """.trimIndent()
            val expectedTable = """
                ┌───┬────────────────────────────────┬─────┬─────────────────┬─┬────┬─────┬──────┬────────────────┬─────────────────────┬────────┬─────┬──────────────────────┬─┐
                │[F]│                                │     │[+] trachten_naar│ │    │     │      │                │                     │        │     │                      │ │
                │   │Hoe_zoet_moet_nochtans_zijn_dit_│<lb/>│[-] werven_om    │_│een_│     │vrouw │,_de_           │ongewisheid_         │vóór_de_│<lb/>│liefelijke_toestemming│!│
                ├───┼────────────────────────────────┼─────┼─────────────────┼─┼────┼─────┼──────┼────────────────┼─────────────────────┼────────┼─────┼──────────────────────┼─┤
                │[Q]│                                │     │[+] trachten_naar│ │    │     │      │                │                     │        │     │                      │ │
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
            val trachtenNaar = CollationGraphAssert.textNodeSketch()
                    .withWitnessSegmentSketch("F", "trachten naar")
                    .withWitnessSegmentSketch("Q", "trachten naar")
                    .withWitnessSegmentSketch("Z", "trachten naar ")
            val wervenOm = CollationGraphAssert.textNodeSketch()
                    .withWitnessSegmentSketch("F", "werven om")
                    .withWitnessSegmentSketch("Q", "werven om")
            assertThat(collationGraph)
                    .containsTextNodesMatching(n1, n2, n3, n4, n5, n6, trachtenNaar, wervenOm)
            assertThat(collationGraph)
                    .containsMarkupNodesMatching(
                            CollationGraphAssert.markupNodeSketch("F", "text"),
                            CollationGraphAssert.markupNodeSketch("Q", "text"),
                            CollationGraphAssert.markupNodeSketch("Z", "text"))
            val fDel = CollationGraphAssert.markupNodeSketch("F", "del")
            val qAdd = CollationGraphAssert.markupNodeSketch("Q", "add")
            assertThat(collationGraph).hasTextNodeMatching(wervenOm).withMarkupNodesMatching(fDel)
            assertThat(collationGraph).hasMarkupNodeMatching(qAdd).withTextNodesMatching(trachtenNaar)
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
            log.info("Collating took {} ms.", duration)
            val markupBeforeJoin = collation.markupList.toSet()
            //    LOG.info("before join: collation markup = {}",
            // collation.getMarkupStream().map(Markup::toString).sorted().collect(toList()));
            collation = CollationGraphNodeJoiner.join(collation)
            val markupAfterJoin = collation.markupList.toSet()
            //    LOG.info("after join: collation markup = {}",
            // collation.getMarkupStream().map(Markup::toString).sorted().collect(toList()));
            assertThat(markupAfterJoin).containsExactlyElementsOf(markupBeforeJoin)

            val dot = CollationGraphVisualizer.toDot(collation, emphasizeWhitespace = true, hideMarkup = false)
            log.info("dot=\n{}", dot)
            writeGraph(dot, "graph")
            assertThat(dot).isEqualTo(expectedDot)

            val table = CollationGraphVisualizer.toTableASCII(collation, true)
                    .replace(" ", " ")
            log.info("table=\n{}", table)
            assertThat(table).isEqualTo(expectedTable.replace("\n", System.lineSeparator()))

            val html = CollationGraphVisualizer.toTableHTML(collation, true)
            log.debug("html=\n{}", html)

            return collation
        }
    }

    @Test
    @Timeout(10000)
    fun permute() {
        val permute1 = HyperCollator.permute(3)
        log.info("permute={}", visualize(permute1))
        assertThat(Sets.newHashSet(permute1)).hasSameSizeAs(permute1)
        assertThat(permute1).hasSize(3)
        val permute2 = HyperCollator.permute(4)
        log.info("permute={}", visualize(permute2))
        assertThat(Sets.newHashSet(permute2)).hasSameSizeAs(permute2)
        assertThat(permute2).hasSize(6)
        val permute3 = HyperCollator.permute(10)
        log.info("permute={}", visualize(permute3))
        assertThat(Sets.newHashSet(permute3)).hasSameSizeAs(permute3)
        assertThat(permute3).hasSize(45)
    }

    @Test
    @Timeout(10000)
    fun potential_matches_1() {
        val importer = XMLImporter()
        val siglum1 = "A"
        val w1 = importer.importXML(siglum1, "<x>the black cat</x>")
        val siglum2 = "B"
        val w2 = importer.importXML(siglum2, "<x>the blue dog</x>")
        val siglum3 = "C"
        val w3 = importer.importXML(siglum3, "<x>the black dog</x>")
        val witnesses = listOf(w1, w2, w3)
        val rankings = witnesses.map { VariantWitnessGraphRanking.of(it) }
        val allPotentialMatches = hyperCollator.getPotentialMatches(witnesses, rankings)
        log.info("allPotentialMatches={}", allPotentialMatches)
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
        assertThat(matchStrings).containsOnly(match1, match2, match3, match4, match5, match6, match7, match8)

        val sortAndFilterMatchesByWitness = hyperCollator.sortAndFilterMatchesByWitness(
                allPotentialMatches, listOf(siglum1, siglum2, siglum3))
        log.info("sortAndFilterMatchesByWitness={}", sortAndFilterMatchesByWitness)
        assertThat(sortAndFilterMatchesByWitness).containsOnlyKeys(siglum1, siglum2, siglum3)

        val listA = sortAndFilterMatchesByWitness.stringList(siglum1)
        assertThat(listA).containsOnly(match1, match2, match3, match6, match7)

        val listB = sortAndFilterMatchesByWitness.stringList(siglum2)
        assertThat(listB).containsOnly(match4, match1, match5, match6, match8)

        val listC = sortAndFilterMatchesByWitness.stringList(siglum3)
        assertThat(listC).containsOnly(match4, match2, match3, match5, match7, match8)
    }

    private fun Match.pretty(): String =
            toString() + " - " + tokenVertexList.map {
                when (it) {
                    is SimpleTokenVertex -> "'" + it.content + "'"
                    is EndTokenVertex -> "</end>"
                    else -> "<somethingelse/>"
                }
            }.first()

    @Test
    fun min_or_null() {
        val l = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
        val m = l.minOrNull()
        assertThat(m).isEqualTo(1)

        val e = listOf<Int>()
        val n = e.minOrNull()
        assertThat(n).isNull()
    }

    companion object {
        private val log = LoggerFactory.getLogger(HyperCollateTest::class.java)
        val hyperCollator = HyperCollator()

        private fun Map<String, List<Match>>.stringList(key: String): List<String> =
                (this[key] ?: error("key $key not found in sortAndFilterMatchesByWitness"))
                        .map(Match::toString)

        private fun visualize(list: List<Tuple<Int>>): String =
                list.joinToString("") { format("<{0},{1}>", it.left, it.right) }

    }

}
