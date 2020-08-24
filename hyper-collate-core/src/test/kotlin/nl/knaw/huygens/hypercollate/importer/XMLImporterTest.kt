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

import nl.knaw.huygens.hypercollate.HyperCollateTest
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph
import nl.knaw.huygens.hypercollate.tools.DotFactory
import nl.knaw.huygens.hypercollate.tools.TokenMerger
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.stream.Collectors

class XMLImporterTest : HyperCollateTest() {
    @Test
    fun testHoeLanger() {
        val importer = XMLImporter()
        //    VariantWitnessGraph wg0 = importer.importXML("A", "<xml>kon den vorst <del>min en
        // min</del><add>hoe langer hoe minder</add> bekoren.</xml>");
        //    VariantWitnessGraph wg0 = importer.importXML("A", "<xml><s><del>Dit kwam van
        // een</del><del><add>Gevolg van een</add></del><add>De</add>te streng doorgedreven
        // rationalisatie<add>had dit met zich meegebracht</add>.</s></xml>");
        //    VariantWitnessGraph wg0 = importer.importXML("A", "<xml><del>Dit kwam van
        // een</del><del><add>Gevolg van een</add></del><add>De</add> te streng doorgedreven
        // rationalisatie</xml>");
        val wg0 = importer.importXML(
                "A",
                "<xml><del>Dit kwam van een</del><del><add>Gevolg van een</add></del><add>De</add></xml>")
        //    VariantWitnessGraph wg0 = importer.importXML("A", "<xml><del>Dit kwam van
        // een</del><add><del>Gevolg van een</del><add>De</add></add> te streng doorgedreven
        // rationalisatie</xml>");
        //    VariantWitnessGraph wg0 = importer.importXML("A", "<xml><subst><del>Dit kwam van
        // een</del><del><add>Gevolg van een</add></del><add>De</add></subst> te streng doorgedreven
        // rationalisatie</xml>");
        visualize(wg0)
    }

    @Test
    fun testImportFromString() {
        val importer = XMLImporter()
        val wg0 = importer.importXML("A", "<xml>Mondays are <del>well good</del><add>def bad</add>!</xml>")
        val expectedDot = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            A_000 [label=<Mondays&#9251;are&#9251;<br/><i>A: /xml</i>>]
            A_002 [label=<well&#9251;good<br/><i>A: /xml/del</i>>]
            A_004 [label=<def&#9251;bad<br/><i>A: /xml/add</i>>]
            A_006 [label=<!<br/><i>A: /xml</i>>]
            end [label="";shape=doublecircle,rank=middle]
            A_000->A_002
            A_000->A_004
            A_002->A_006
            A_004->A_006
            A_006->end
            begin->A_000
            }
            """.trimIndent()
        verifyDotExport(wg0, expectedDot)
    }

    @Test
    fun testImportFromFile() {
        val importer = XMLImporter()
        val resourceAsStream = javaClass.getResourceAsStream("/witness.xml")
        val wg0 = importer.importXML("A", resourceAsStream)
        val expected = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            A_000 [label=<De&#9251;vent&#9251;was&#9251;woedend&#9251;en&#9251;maakte&#9251;<br/><i>A: /text/s</i>>]
            A_006 [label=<Shiriar<br/><i>A: /text/s/del</i>>]
            A_007 [label=<den&#9251;bedremmelden&#9251;<br/><i>A: /text/s/add</i>>]
            A_011 [label=<&#9251;uit&#9251;voor&#9251;"lompen&#9251;boer".<br/><i>A: /text/s</i>>]
            A_009 [label=<man<br/><i>A: /text/s/add/del</i>>]
            A_010 [label=<Sultan<br/><i>A: /text/s/add/add</i>>]
            end [label="";shape=doublecircle,rank=middle]
            A_000->A_006
            A_000->A_007
            A_006->A_011
            A_007->A_009
            A_007->A_010
            A_009->A_011
            A_010->A_011
            A_011->end
            begin->A_000
            }
            """.trimIndent()
        verifyDotExport(wg0, expected)
    }

    @Test
    fun testDelWithoutAdd() {
        val importer = XMLImporter()
        val wg0 = importer.importXML("A", "<xml>Ja toch! <del>Niet dan?</del> Ik dacht het wel!</xml>")
        val expectedDot = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            A_000 [label=<Ja&#9251;toch!&#9251;<br/><i>A: /xml</i>>]
            A_003 [label=<Niet&#9251;dan?<br/><i>A: /xml/del</i>>]
            A_006 [label=<&#9251;Ik&#9251;dacht&#9251;het&#9251;wel!<br/><i>A: /xml</i>>]
            end [label="";shape=doublecircle,rank=middle]
            A_000->A_003
            A_000->A_006
            A_003->A_006
            A_006->end
            begin->A_000
            }
            """.trimIndent()
        verifyDotExport(wg0, expectedDot)
    }

    @Disabled
    @Test
    fun testDoubleDel() {
        val importer = XMLImporter()
        val wg0 = importer.importXML("A", "<xml>word1 <del>word2</del><del>word3</del> word4</xml>")
        val expectedDot = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            A_000 [label=<word1&#9251;<br/><i>A: /xml</i>>]
            A_001 [label=<word2<br/><i>A: /xml/del</i>>]
            A_003 [label=<&#9251;word4<br/><i>A: /xml</i>>]
            A_002 [label=<word3<br/><i>A: /xml/del</i>>]
            end [label="";shape=doublecircle,rank=middle]
            A_000->A_001
            A_000->A_002
            A_000->A_003
            A_001->A_002
            A_001->A_003
            A_002->A_003
            A_003->end
            begin->A_000
            }
            """.trimIndent()
        verifyDotExport(wg0, expectedDot)
    }

    @Disabled
    @Test
    fun testTripleDel() {
        val importer = XMLImporter()
        val wg0 = importer.importXML(
                "A", "<xml>word1 <del>word2</del><del>word3</del><del>word4</del> word5</xml>")
        val expectedDot = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            A_000 [label=<word1&#9251;<br/><i>A: /xml</i>>]
            A_001 [label=<word2<br/><i>A: /xml/del</i>>]
            A_004 [label=<&#9251;word5<br/><i>A: /xml</i>>]
            A_002 [label=<word3<br/><i>A: /xml/del</i>>]
            end [label="";shape=doublecircle,rank=middle]
            A_003 [label=<word4<br/><i>A: /xml/del</i>>]
            A_000->A_001
            A_000->A_002
            A_000->A_003
            A_000->A_004
            A_001->A_002
            A_001->A_003
            A_001->A_004
            A_002->A_003
            A_002->A_004
            A_003->A_004
            A_004->end
            begin->A_000
            }
            """.trimIndent()
        verifyDotExport(wg0, expectedDot)
    }

    @Test
    fun testDelWithoutAddAtTheEnd() {
        val importer = XMLImporter()
        val wg0 = importer.importXML("A", "<xml>And they lived happily ever after. <del>Or not.</del></xml>")
        val expectedDot = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            A_000 [label=<And&#9251;they&#9251;lived&#9251;happily&#9251;ever&#9251;after.&#9251;<br/><i>A: /xml</i>>]
            A_007 [label=<Or&#9251;not.<br/><i>A: /xml/del</i>>]
            end [label="";shape=doublecircle,rank=middle]
            A_000->A_007
            A_000->end
            A_007->end
            begin->A_000
            }
            """.trimIndent()
        verifyDotExport(wg0, expectedDot)
    }

    @Test
    fun testAddWithoutDel() {
        val importer = XMLImporter()
        val wg0 = importer.importXML("A", "<xml>Eenie meeny <add>miny</add> moe.</xml>")
        val expectedDot = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            A_000 [label=<Eenie&#9251;meeny&#9251;<br/><i>A: /xml</i>>]
            A_002 [label=<miny<br/><i>A: /xml/add</i>>]
            A_003 [label=<&#9251;moe.<br/><i>A: /xml</i>>]
            end [label="";shape=doublecircle,rank=middle]
            A_000->A_002
            A_000->A_003
            A_002->A_003
            A_003->end
            begin->A_000
            }
            """.trimIndent()
        verifyDotExport(wg0, expectedDot)
    }

    @Test
    fun testAddWithoutDelAtTheEnd() {
        val importer = XMLImporter()
        val wg0 = importer.importXML("A", "<xml>The End. <add>After credits.</add></xml>")
        val expectedDot = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            A_000 [label=<The&#9251;End.&#9251;<br/><i>A: /xml</i>>]
            A_003 [label=<After&#9251;credits.<br/><i>A: /xml/add</i>>]
            end [label="";shape=doublecircle,rank=middle]
            A_000->A_003
            A_000->end
            A_003->end
            begin->A_000
            }
            """.trimIndent()
        verifyDotExport(wg0, expectedDot)
    }

    @Test
    fun testAppRdg() {
        val importer = XMLImporter()
        val wg0 = importer.importXML(
                "A",
                "<xml>Sinterklaas en"
                        + "<app>"
                        + "<rdg wit=\"a\">Zwarte Piet</rdg>"
                        + "<rdg wit=\"b\">Roetpiet</rdg>"
                        + "</app> zijn weer aangekomen.</xml>")
        val expectedDot = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            A_000 [label=<Sinterklaas&#9251;en<br/><i>A: /xml</i>>]
            A_002 [label=<Zwarte&#9251;Piet<br/><i>A: /xml/app/rdg</i>>]
            A_004 [label=<Roetpiet<br/><i>A: /xml/app/rdg</i>>]
            A_005 [label=<&#9251;zijn&#9251;weer&#9251;aangekomen.<br/><i>A: /xml</i>>]
            end [label="";shape=doublecircle,rank=middle]
            A_000->A_002
            A_000->A_004
            A_002->A_005
            A_004->A_005
            A_005->end
            begin->A_000
            }
            """.trimIndent()
        verifyDotExport(wg0, expectedDot)
    }

    @Test
    fun testAppRdg2() {
        val importer = XMLImporter()
        val wg0 = importer.importXML(
                "A",
                """
                    <xml>Sinterklaas en<app>
                    <rdg wit="a">Zwarte Piet</rdg>
                    <rdg wit="b">Roetpiet</rdg>
                    </app> zijn weer aangekomen.</xml>
                    """.trimIndent())
        val expectedDot = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            A_000 [label=<Sinterklaas&#9251;en<br/><i>A: /xml</i>>]
            A_002 [label=<Zwarte&#9251;Piet<br/><i>A: /xml/app/rdg</i>>]
            A_004 [label=<Roetpiet<br/><i>A: /xml/app/rdg</i>>]
            A_005 [label=<&#9251;zijn&#9251;weer&#9251;aangekomen.<br/><i>A: /xml</i>>]
            end [label="";shape=doublecircle,rank=middle]
            A_000->A_002
            A_000->A_004
            A_002->A_005
            A_004->A_005
            A_005->end
            begin->A_000
            }
            """.trimIndent()
        verifyDotExport(wg0, expectedDot)
    }

    @Test
    fun testWitnessFOrderAppRdgBordalejo() {
        val importer = XMLImporter()
        val wg0 = importer.importXML(
                "F",
                """<text>
    <s>De vent was woedend en maakte 
        <app>
            <rdg type="l1">Shiriar</rdg>
            <rdg type="lit"><hi rend="strike">Shiriar</hi></rdg>
        </app> den bedremmelden Sultan uit
        voor "lompen boer".</s>
</text>""")
        val expectedDot = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            F_000 [label=<De&#9251;vent&#9251;was&#9251;woedend&#9251;en&#9251;maakte&#9251;<br/><i>F: /text/s</i>>]
            F_006 [label=<Shiriar<br/><i>F: /text/s/app/rdg</i>>]
            F_007 [label=<&#9251;den&#9251;bedremmelden&#9251;Sultan&#9251;uit&#x21A9;<br/>&#9251;voor&#9251;"lompen&#9251;boer".<br/><i>F: /text/s</i>>]
            end [label="";shape=doublecircle,rank=middle]
            F_000->F_006
            F_006->F_007
            F_007->end
            begin->F_000
            }
            """.trimIndent()
        verifyDotExport(wg0, expectedDot, "witness-f-order-app-rdg-bordalejo")
    }

    @Test
    fun testWitnessFOrderAppRdgVincent() {
        val importer = XMLImporter()
        val wg0 = importer.importXML(
                "F",
                """<text>
    <s>De vent was woedend en maakte 
        <app>
            <rdg><del type="instantCorrection">Shiriar</del></rdg>
            <rdg type="lit"><hi rend="strike">Shiriar</hi></rdg>
        </app> den bedremmelden Sultan uit
        voor "lompen boer".</s>
</text>""")
        val expectedDot = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            F_000 [label=<De&#9251;vent&#9251;was&#9251;woedend&#9251;en&#9251;maakte&#9251;<br/><i>F: /text/s</i>>]
            F_006 [label=<Shiriar<br/><i>F: /text/s/app/rdg/del</i>>]
            F_007 [label=<&#9251;den&#9251;bedremmelden&#9251;Sultan&#9251;uit&#x21A9;<br/>&#9251;voor&#9251;"lompen&#9251;boer".<br/><i>F: /text/s</i>>]
            end [label="";shape=doublecircle,rank=middle]
            F_000->F_006
            F_006->F_007
            F_007->end
            begin->F_000
            }
            """.trimIndent()
        verifyDotExport(wg0, expectedDot, "witness-f-order-app-rdg-vincent")
    }

    @Test
    fun testWitnessQOrderAppRdgBordalejo() {
        val importer = XMLImporter()
        val wg0 = importer.importXML(
                "Q",
                """<text>
    <s>De vent was woedend en maakte 
        <app>
            <rdg type="l1">Shiriar</rdg>
            <rdg type="l2">den bedremmelden man</rdg>
            <rdg type="l3">den bedremmelden Sultan</rdg>
            <rdg type="lit">
                <hi rend="strike">Shiriar</hi>
                <hi rend="margin">den bedremmelden</hi>
                <hi rend="strike">man</hi><hi rend="supralinear">Sultan</hi>
            </rdg>
        </app>uit voor <q>"lompen boer"</q>.</s>
</text>""")
        val expectedDot = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            Q_000 [label=<De&#9251;vent&#9251;was&#9251;woedend&#9251;en&#9251;maakte&#9251;<br/><i>Q: /text/s</i>>]
            Q_006 [label=<Shiriar<br/><i>Q: /text/s/app/rdg</i>>]
            Q_007 [label=<den&#9251;bedremmelden&#9251;man<br/><i>Q: /text/s/app/rdg</i>>]
            Q_010 [label=<den&#9251;bedremmelden&#9251;Sultan<br/><i>Q: /text/s/app/rdg</i>>]
            Q_013 [label=<uit&#9251;voor&#9251;<br/><i>Q: /text/s</i>>]
            Q_015 [label=<"lompen&#9251;boer"<br/><i>Q: /text/s/q</i>>]
            Q_017 [label=<.<br/><i>Q: /text/s</i>>]
            end [label="";shape=doublecircle,rank=middle]
            Q_000->Q_006
            Q_000->Q_007
            Q_000->Q_010
            Q_006->Q_013
            Q_007->Q_013
            Q_010->Q_013
            Q_013->Q_015
            Q_015->Q_017
            Q_017->end
            begin->Q_000
            }
            """.trimIndent()
        verifyDotExport(wg0, expectedDot, "witness-q-order-app-rdg-bordalejo")
    }

    @Test
    fun testWitnessQOrderAppRdgVincent() {
        val importer = XMLImporter()
        val wg0 = importer.importXML(
                "Q",
                """<text>    
    <s>De vent was woedend en maakte <app>
        <rdg><del>Shiriar</del></rdg>
        <rdg>
            <add>den bedremmelden 
                <app><rdg><del>man</del></rdg>
                <rdg><add>Sultan</add></rdg></app>
            </add>
        </rdg>
        <rdg type="lit"><hi rend="strike">Shiriar</hi>
        <hi rend="margin">den bedremmelden</hi>
        <hi rend="strike">man</hi><hi rend="supralinear">Sultan</hi>
        </rdg>
    </app> 
        uit voor <q>"lompen boer"</q>.</s>
</text>""")
        val expectedDot = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            Q_000 [label=<&#9251;<br/><i>Q: /text</i>>]
            Q_001 [label=<De&#9251;vent&#9251;was&#9251;woedend&#9251;en&#9251;maakte&#9251;<br/><i>Q: /text/s</i>>]
            Q_007 [label=<Shiriar<br/><i>Q: /text/s/app/rdg/del</i>>]
            Q_008 [label=<den&#9251;bedremmelden&#9251;&#x21A9;<br/>&#9251;<br/><i>Q: /text/s/app/rdg/add</i>>]
            Q_012 [label=<&#9251;uit&#9251;voor&#9251;<br/><i>Q: /text/s</i>>]
            Q_010 [label=<man<br/><i>Q: /text/s/app/rdg/add/app/rdg/del</i>>]
            Q_011 [label=<Sultan<br/><i>Q: /text/s/app/rdg/add/app/rdg/add</i>>]
            Q_015 [label=<"lompen&#9251;boer"<br/><i>Q: /text/s/q</i>>]
            Q_017 [label=<.<br/><i>Q: /text/s</i>>]
            end [label="";shape=doublecircle,rank=middle]
            Q_000->Q_001
            Q_001->Q_007
            Q_001->Q_008
            Q_007->Q_012
            Q_008->Q_010
            Q_008->Q_011
            Q_010->Q_012
            Q_011->Q_012
            Q_012->Q_015
            Q_015->Q_017
            Q_017->end
            begin->Q_000
            }
            """.trimIndent()
        verifyDotExport(wg0, expectedDot, "witness-q-order-app-rdg-vincent")
    }

    @Test
    fun testWitnessFHierarchyAppRdgBordalejo() {
        val importer = XMLImporter()
        val wg0 = importer.importXML(
                "F",
                """<text>
    <s>Hoe zoet moet nochtans zijn dit <lb/>
        <app>
            <rdg type="l1">werven om</rdg>
            <rdg type="l2">trachten naar</rdg>
            <rdg type="lit"><hi rend="strike">werven om</hi> <hi rend="supralinear">trachten naar</hi></rdg>
        </app> 
        een vrouw, de ongewisheid vóór de <lb/>liefelijke toestemming!</s>
</text>""")
        val expectedDot = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            F_000 [label=<Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/><i>F: /text/s</i>>]
            F_006 [label=<<br/><i>F: /text/s/lb</i>>]
            F_007 [label=<werven&#9251;om<br/><i>F: /text/s/app/rdg</i>>]
            F_009 [label=<trachten&#9251;naar<br/><i>F: /text/s/app/rdg</i>>]
            F_011 [label=<&#9251;een&#9251;vrouw,&#9251;de&#9251;ongewisheid&#9251;vóór&#9251;de&#9251;<br/><i>F: /text/s</i>>]
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
        verifyDotExport(wg0, expectedDot, "witness-f-hierarchy-app-rdg-bordalejo")
    }

    @Test
    fun testWitnessFHierarchyAppRdgVincent() {
        val importer = XMLImporter()
        val wg0 = importer.importXML(
                "F",
                """<text>
    <s>Hoe zoet moet nochtans zijn dit <lb/>
        <app>
            <rdg><del>werven om</del></rdg>
            <rdg><add>trachten naar</add></rdg>
            <rdg type="lit"><hi rend="strike">werven om</hi> <hi rend="supralinear">trachten naar</hi></rdg>
        </app> 
        een vrouw, de ongewisheid vóór de <lb/>liefelijke toestemming!</s>
</text>""")
        val expectedDot = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            F_000 [label=<Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/><i>F: /text/s</i>>]
            F_006 [label=<<br/><i>F: /text/s/lb</i>>]
            F_007 [label=<werven&#9251;om<br/><i>F: /text/s/app/rdg/del</i>>]
            F_009 [label=<trachten&#9251;naar<br/><i>F: /text/s/app/rdg/add</i>>]
            F_011 [label=<&#9251;een&#9251;vrouw,&#9251;de&#9251;ongewisheid&#9251;vóór&#9251;de&#9251;<br/><i>F: /text/s</i>>]
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
        verifyDotExport(wg0, expectedDot, "witness-f-hierarchy-app-rdg-vincent")
    }

    @Test
    fun testWitnessQHierarchyAppRdgBordalejo() {
        val importer = XMLImporter()
        val wg0 = importer.importXML(
                "F",
                """<text>
    <s>Hoe zoet moet nochtans zijn dit 
        <app>
            <rdg type="l1">werven om</rdg>
            <rdg type="l2">trachten naar</rdg>
            <rdg type="lit"><hi rend="strike">werven om</hi> <hi rend="supralinear">trachten naar</hi></rdg>
        </app> 
        een <lb/>vrouw !</s>
    <s>Die dagen van nerveuze verwachting vóór de liefelijke toestemming.</s>
</text>""")
        val expectedDot = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            F_000 [label=<Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/><i>F: /text/s</i>>]
            F_006 [label=<werven&#9251;om<br/><i>F: /text/s/app/rdg</i>>]
            F_008 [label=<trachten&#9251;naar<br/><i>F: /text/s/app/rdg</i>>]
            F_010 [label=<&#9251;een&#9251;<br/><i>F: /text/s</i>>]
            F_012 [label=<<br/><i>F: /text/s/lb</i>>]
            F_013 [label=<vrouw&#9251;!<br/><i>F: /text/s</i>>]
            F_015 [label=<Die&#9251;dagen&#9251;van&#9251;nerveuze&#9251;verwachting&#9251;vóór&#9251;de&#9251;liefelijke&#9251;toestemming.<br/><i>F: /text/s</i>>]
            end [label="";shape=doublecircle,rank=middle]
            F_000->F_006
            F_000->F_008
            F_006->F_010
            F_008->F_010
            F_010->F_012
            F_012->F_013
            F_013->F_015
            F_015->end
            begin->F_000
            }
            """.trimIndent()
        verifyDotExport(wg0, expectedDot, "witness-q-hierarchy-app-rdg-bordalejo")
    }

    @Test
    fun testWitnessQHierarchyAppRdgVincent() {
        val importer = XMLImporter()
        val wg0 = importer.importXML(
                "F",
                """<text>
    <s>Hoe zoet moet nochtans zijn dit 
        <app>
            <rdg><del>werven om</del></rdg>
            <rdg><add>trachten naar</add></rdg>
            <rdg type="lit"><hi rend="strike">werven om</hi> <hi rend="supralinear">trachten naar</hi></rdg>
        </app> 
        een <lb/>vrouw !</s>
        <s>Die dagen van nerveuze verwachting vóór de liefelijke toestemming.</s>
</text>""")
        val expectedDot = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            F_000 [label=<Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/><i>F: /text/s</i>>]
            F_006 [label=<werven&#9251;om<br/><i>F: /text/s/app/rdg/del</i>>]
            F_008 [label=<trachten&#9251;naar<br/><i>F: /text/s/app/rdg/add</i>>]
            F_010 [label=<&#9251;een&#9251;<br/><i>F: /text/s</i>>]
            F_012 [label=<<br/><i>F: /text/s/lb</i>>]
            F_013 [label=<vrouw&#9251;!<br/><i>F: /text/s</i>>]
            F_015 [label=<Die&#9251;dagen&#9251;van&#9251;nerveuze&#9251;verwachting&#9251;vóór&#9251;de&#9251;liefelijke&#9251;toestemming.<br/><i>F: /text/s</i>>]
            end [label="";shape=doublecircle,rank=middle]
            F_000->F_006
            F_000->F_008
            F_006->F_010
            F_008->F_010
            F_010->F_012
            F_012->F_013
            F_013->F_015
            F_015->end
            begin->F_000
            }
            """.trimIndent()
        verifyDotExport(wg0, expectedDot, "witness-q-hierarchy-app-rdg-vincent")
    }

    @Test
    fun testSingleAdd() {
        val importer = XMLImporter()
        val a = importer.importXML(
                "a", "<p><s>&amp; himself corrected <add>and augmented</add> them</s></p>")
        val markups = a.markupStream.collect(Collectors.toList())
        Assertions.assertThat(markups).hasSize(3)
        val dot = DotFactory(true).fromVariantWitnessGraphColored(a)
        val expectedDot = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            node [style="filled";fillcolor="white"]
            begin [label="";shape=doublecircle,rank=middle]
            subgraph cluster_0 {
            label=<<i><b>p</b></i>>
            graph[style="rounded,filled";fillcolor="yellow"]
            subgraph cluster_1 {
            label=<<i><b>s</b></i>>
            graph[style="rounded,filled";fillcolor="orange"]
            a_000 [label=<&amp;>]
            a_001 [label=<&#9251;>]
            a_002 [label=<himself&#9251;>]
            a_003 [label=<corrected&#9251;>]
            subgraph cluster_2 {
            label=<<i><b>add</b></i>>
            graph[style="rounded,filled";fillcolor="#9aed7d"]
            a_004 [label=<and&#9251;>]
            a_005 [label=<augmented>]
            }
            a_006 [label=<&#9251;>]
            a_007 [label=<them>]
            }
            }
            end [label="";shape=doublecircle,rank=middle]
            a_000->a_001
            a_001->a_002
            a_002->a_003
            a_003->a_004
            a_003->a_006
            a_004->a_005
            a_005->a_006
            a_006->a_007
            a_007->end
            begin->a_000
            }
            """.trimIndent()
        Assertions.assertThat(dot).isEqualTo(expectedDot)
    }

    private fun visualize(vwg: VariantWitnessGraph) {
        val dot0s = DotFactory(true).fromVariantWitnessGraphSimple(vwg)
        LOG.info("unjoined simple:\n{}", dot0s)
        val dot0c = DotFactory(true).fromVariantWitnessGraphColored(vwg)
        LOG.info("unjoined colored:\n{}", dot0c)
        val joined = TokenMerger.merge(vwg)
        val dot1s = DotFactory(true).fromVariantWitnessGraphSimple(joined)
        LOG.info("joined simple:\n{}", dot1s)
        val dot1c = DotFactory(true).fromVariantWitnessGraphColored(joined)
        LOG.info("joined colored:\n{}", dot1c)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(XMLImporterTest::class.java)
    }
}
