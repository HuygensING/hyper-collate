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
import nl.knaw.huygens.hypercollate.collator.VariantWitnessGraphRanking
import nl.knaw.huygens.hypercollate.importer.XMLImporter.Companion.normalizedSiglum
import nl.knaw.huygens.hypercollate.model.BranchSet
import nl.knaw.huygens.hypercollate.model.SimpleTokenVertex
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph
import nl.knaw.huygens.hypercollate.tools.DotFactory
import nl.knaw.huygens.hypercollate.tools.DotFactory.Companion.branchId
import nl.knaw.huygens.hypercollate.tools.TokenMerger
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class XMLImporterTest : HyperCollateTest() {

    @Test
    fun witness_branch_identifier_from_xpath() {
        assertThat("/xml/subst/add".branchId()).isEqualTo("+")
        assertThat("/xml/subst/del".branchId()).isEqualTo("-")
        assertThat("/xml/subst/add/del".branchId()).isEqualTo("+-")
        assertThat("/xml/subst/add/add".branchId()).isEqualTo("++")
        assertThat("/xml/app/rdg[@varSeq='1']".branchId()).isEqualTo("1")
        assertThat("/xml/app/rdg[@type='l1']/add".branchId()).isEqualTo("l1+")
        assertThat("/xml/app/rdg[@wit='a'".branchId()).isEqualTo("a")
    }

    @Test
    fun witness_with_app_rdg_has_rdg_identifier_in_xpath() {
        val importer = XMLImporter()
        val xmlString = """
            <xml>Mondays are <app>
              <rdg varSeq="1">deaf bat</rdg>
              <rdg varSeq="2">def bad</rdg>
            </app>!</xml>
            """.trimIndent()
                .replace("""\s+""".toRegex(), " ")
                .replace("> <", "><")
        println(xmlString)
        val wg0: VariantWitnessGraph = importer.importXML("A", xmlString)
        val deafTokenVertex = wg0.vertices().filterIsInstance<SimpleTokenVertex>().first { it.normalizedContent == "deaf" }
        assertThat(deafTokenVertex.parentXPath).isEqualTo("/xml/app/rdg[@varSeq='1']")
        val badTokenVertex = wg0.vertices().filterIsInstance<SimpleTokenVertex>().first { it.normalizedContent == "bad" }
        assertThat(badTokenVertex.parentXPath).isEqualTo("/xml/app/rdg[@varSeq='2']")

        val expectedDot = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            vA_000 [label=<Mondays&#9251;are&#9251;<br/><i>A: /xml</i>>]
            vA_002 [label=<deaf&#9251;bat<br/><i>A: /xml/app/rdg[@varSeq='1']</i>>]
            vA_004 [label=<def&#9251;bad<br/><i>A: /xml/app/rdg[@varSeq='2']</i>>]
            vA_006 [label=<!<br/><i>A: /xml</i>>]
            end [label="";shape=doublecircle,rank=middle]
            begin->vA_000
            vA_000->vA_002
            vA_000->vA_004
            vA_002->vA_006
            vA_004->vA_006
            vA_006->end
            }
            """.trimIndent()
        verifyDotExport(wg0, expectedDot)
    }

    @Disabled
    @Test
    fun witness_with_subst_has_expected_branchset() {
        val importer = XMLImporter()
        val xmlString = """
            <xml>Mondays are <subst>
              <sic>deaf bat</sic>
              <corr>def bad</corr>
            </subst>!</xml>
            """.trimIndent()
                .replace("""\s+""".toRegex(), " ")
                .replace("> <", "><")
        println(xmlString)

        val wg0: VariantWitnessGraph = importer.importXML("A", xmlString)
        val branchSets: List<BranchSet> = wg0.branchSets
        assertThat(branchSets).hasSize(1)
        val branchSet0 = branchSets[0]
        assertThat(branchSet0).hasSize(2)

        val branch0 = branchSet0[0]
        assertThat(branch0.map { it.content }).containsExactly("deaf ", "bat")
        assertThat(branch0.map { it.indexNumber }).containsExactly(3L, 4L)

        val branch1 = branchSet0[1]
        assertThat(branch1.map { it.content }).containsExactly("def ", "bad")
        assertThat(branch1.map { it.indexNumber }).containsExactly(5L, 6L)

        val expectedDot = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            vA_000 [label=<Mondays&#9251;are&#9251;<br/><i>A: /xml</i>>]
            vA_002 [label=<deaf&#9251;bat<br/><i>A: /xml/subst/sic</i>>]
            vA_004 [label=<def&#9251;bad<br/><i>A: /xml/subst/corr</i>>]
            vA_006 [label=<!<br/><i>A: /xml</i>>]
            end [label="";shape=doublecircle,rank=middle]
            begin->vA_000
            vA_000->vA_002
            vA_000->vA_004
            vA_002->vA_006
            vA_004->vA_006
            vA_006->end
            }
            """.trimIndent()
        verifyDotExport(wg0, expectedDot)
    }

    @Test
    fun witness_with_subst() {
        val importer = XMLImporter()
        val xmlString = """
            <xml>Mondays are <subst>
              <sic>deaf bat</sic>
              <corr>def bad</corr>
            </subst>!</xml>
            """.trimIndent()
                .replace("""\s+""".toRegex(), " ")
                .replace("> <", "><")
        println(xmlString)
        val wg0: VariantWitnessGraph = importer.importXML("A", xmlString)
        val branchSetRankingRanges: Map<Int, IntRange> = wg0.showBranchSetRanges()
        assertThat(branchSetRankingRanges).containsOnlyKeys(0)

        val expectedDot = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            vA_000 [label=<Mondays&#9251;are&#9251;<br/><i>A: /xml</i>>]
            vA_002 [label=<deaf&#9251;bat<br/><i>A: /xml/subst/sic</i>>]
            vA_004 [label=<def&#9251;bad<br/><i>A: /xml/subst/corr</i>>]
            vA_006 [label=<!<br/><i>A: /xml</i>>]
            end [label="";shape=doublecircle,rank=middle]
            begin->vA_000
            vA_000->vA_002
            vA_000->vA_004
            vA_002->vA_006
            vA_004->vA_006
            vA_006->end
            }
            """.trimIndent()
        verifyDotExport(wg0, expectedDot)
    }

    private fun VariantWitnessGraph.showBranchSetRanges(): Map<Int, IntRange> {
        val ranking: VariantWitnessGraphRanking = VariantWitnessGraphRanking.of(this)
        val branchSetRankingRanges: Map<Int, IntRange> = ranking.branchSetRankingRanges()
        println(branchSetRankingRanges)
        for ((n, range) in branchSetRankingRanges) {
            val tokensForRange = range.map { (ranking.byRank[it] ?: error("")).map { v -> v.token } }
            println("branchset $n has tokens $tokensForRange")
        }
        return branchSetRankingRanges
    }

    private fun VariantWitnessGraphRanking.branchSetRankingRanges(): Map<Int, IntRange> {
        val map: MutableMap<Int, IntRange> = mutableMapOf()
        val ranks = byRank.keys.sorted()
        var branchSetNum = 0
        var inBranchSet = false
        var branchSetStartRank = 0
        var branchSetEndRank = 0
        for (rank in ranks) {
            if ((byRank[rank] ?: error("")).size > 1) { // multiple vertices at this rank -> in a branchset
                if (!inBranchSet) { // start a new branchset
                    inBranchSet = true
                    branchSetStartRank = rank
                }
            } else {
                if (inBranchSet) {
                    inBranchSet = false
                    branchSetEndRank = rank - 1
                    map[branchSetNum++] = IntRange(branchSetStartRank, branchSetEndRank)
                }
            }
        }
        if (inBranchSet) {
            branchSetEndRank = ranks.size
            map[branchSetNum] = IntRange(branchSetStartRank, branchSetEndRank)
        }
        return map
    }

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
            vA_000 [label=<Mondays&#9251;are&#9251;<br/><i>A: /xml</i>>]
            vA_002 [label=<well&#9251;good<br/><i>A: /xml/del</i>>]
            vA_004 [label=<def&#9251;bad<br/><i>A: /xml/add</i>>]
            vA_006 [label=<!<br/><i>A: /xml</i>>]
            end [label="";shape=doublecircle,rank=middle]
            begin->vA_000
            vA_000->vA_002
            vA_000->vA_004
            vA_002->vA_006
            vA_004->vA_006
            vA_006->end
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
            vA_000 [label=<De&#9251;vent&#9251;was&#9251;woedend&#9251;en&#9251;maakte&#9251;<br/><i>A: /text/s</i>>]
            vA_006 [label=<Shiriar<br/><i>A: /text/s/del</i>>]
            vA_007 [label=<den&#9251;bedremmelden&#9251;<br/><i>A: /text/s/add</i>>]
            vA_011 [label=<&#9251;uit&#9251;voor&#9251;"lompen&#9251;boer".<br/><i>A: /text/s</i>>]
            vA_009 [label=<man<br/><i>A: /text/s/add/del</i>>]
            vA_010 [label=<Sultan<br/><i>A: /text/s/add/add</i>>]
            end [label="";shape=doublecircle,rank=middle]
            begin->vA_000
            vA_000->vA_006
            vA_000->vA_007
            vA_006->vA_011
            vA_007->vA_009
            vA_007->vA_010
            vA_009->vA_011
            vA_010->vA_011
            vA_011->end
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
            vA_000 [label=<Ja&#9251;toch!&#9251;<br/><i>A: /xml</i>>]
            vA_003 [label=<Niet&#9251;dan?<br/><i>A: /xml/del</i>>]
            vA_006 [label=<&#9251;Ik&#9251;dacht&#9251;het&#9251;wel!<br/><i>A: /xml</i>>]
            end [label="";shape=doublecircle,rank=middle]
            begin->vA_000
            vA_000->vA_003
            vA_000->vA_006
            vA_003->vA_006
            vA_006->end
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
            vA_000 [label=<word1&#9251;<br/><i>A: /xml</i>>]
            vA_001 [label=<word2<br/><i>A: /xml/del</i>>]
            vA_003 [label=<&#9251;word4<br/><i>A: /xml</i>>]
            vA_002 [label=<word3<br/><i>A: /xml/del</i>>]
            end [label="";shape=doublecircle,rank=middle]
            begin->vA_000
            vA_000->vA_001
            vA_000->vA_002
            vA_000->vA_003
            vA_001->vA_002
            vA_001->vA_003
            vA_002->vA_003
            vA_003->end
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
            vA_000 [label=<word1&#9251;<br/><i>A: /xml</i>>]
            vA_001 [label=<word2<br/><i>A: /xml/del</i>>]
            vA_004 [label=<&#9251;word5<br/><i>A: /xml</i>>]
            vA_002 [label=<word3<br/><i>A: /xml/del</i>>]
            end [label="";shape=doublecircle,rank=middle]
            vA_003 [label=<word4<br/><i>A: /xml/del</i>>]
            begin->vA_000
            vA_000->vA_001
            vA_000->vA_002
            vA_000->vA_003
            vA_000->vA_004
            vA_001->vA_002
            vA_001->vA_003
            vA_001->vA_004
            vA_002->vA_003
            vA_002->vA_004
            vA_003->vA_004
            vA_004->end
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
            vA_000 [label=<And&#9251;they&#9251;lived&#9251;happily&#9251;ever&#9251;after.&#9251;<br/><i>A: /xml</i>>]
            vA_007 [label=<Or&#9251;not.<br/><i>A: /xml/del</i>>]
            end [label="";shape=doublecircle,rank=middle]
            begin->vA_000
            vA_000->end
            vA_000->vA_007
            vA_007->end
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
            vA_000 [label=<Eenie&#9251;meeny&#9251;<br/><i>A: /xml</i>>]
            vA_002 [label=<miny<br/><i>A: /xml/add</i>>]
            vA_003 [label=<&#9251;moe.<br/><i>A: /xml</i>>]
            end [label="";shape=doublecircle,rank=middle]
            begin->vA_000
            vA_000->vA_002
            vA_000->vA_003
            vA_002->vA_003
            vA_003->end
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
            vA_000 [label=<The&#9251;End.&#9251;<br/><i>A: /xml</i>>]
            vA_003 [label=<After&#9251;credits.<br/><i>A: /xml/add</i>>]
            end [label="";shape=doublecircle,rank=middle]
            begin->vA_000
            vA_000->end
            vA_000->vA_003
            vA_003->end
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
            vA_000 [label=<Sinterklaas&#9251;en<br/><i>A: /xml</i>>]
            vA_002 [label=<Zwarte&#9251;Piet<br/><i>A: /xml/app/rdg[@wit='a']</i>>]
            vA_004 [label=<Roetpiet<br/><i>A: /xml/app/rdg[@wit='b']</i>>]
            vA_005 [label=<&#9251;zijn&#9251;weer&#9251;aangekomen.<br/><i>A: /xml</i>>]
            end [label="";shape=doublecircle,rank=middle]
            begin->vA_000
            vA_000->vA_002
            vA_000->vA_004
            vA_002->vA_005
            vA_004->vA_005
            vA_005->end
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
            vA_000 [label=<Sinterklaas&#9251;en<br/><i>A: /xml</i>>]
            vA_002 [label=<Zwarte&#9251;Piet<br/><i>A: /xml/app/rdg[@wit='a']</i>>]
            vA_004 [label=<Roetpiet<br/><i>A: /xml/app/rdg[@wit='b']</i>>]
            vA_005 [label=<&#9251;zijn&#9251;weer&#9251;aangekomen.<br/><i>A: /xml</i>>]
            end [label="";shape=doublecircle,rank=middle]
            begin->vA_000
            vA_000->vA_002
            vA_000->vA_004
            vA_002->vA_005
            vA_004->vA_005
            vA_005->end
            }
            """.trimIndent()
        verifyDotExport(wg0, expectedDot)
    }

    @Test
    fun testWitnessFOrderAppRdgBordalejo() {
        val importer = XMLImporter()
        val wg0 = importer.importXML(
                "F",
                """
                <text>
                    <s>De vent was woedend en maakte 
                        <app>
                            <rdg type="l1">Shiriar</rdg>
                            <rdg type="lit"><hi rend="strike">Shiriar</hi></rdg>
                        </app> den bedremmelden Sultan uit
                        voor "lompen boer".</s>
                </text>""".trimIndent())
        val expectedDot = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            vF_000 [label=<De&#9251;vent&#9251;was&#9251;woedend&#9251;en&#9251;maakte&#9251;<br/><i>F: /text/s</i>>]
            vF_006 [label=<Shiriar<br/><i>F: /text/s/app/rdg[@type='l1']</i>>]
            vF_007 [label=<&#9251;den&#9251;bedremmelden&#9251;Sultan&#9251;uit&#x21A9;<br/>&#9251;voor&#9251;"lompen&#9251;boer".<br/><i>F: /text/s</i>>]
            end [label="";shape=doublecircle,rank=middle]
            begin->vF_000
            vF_000->vF_006
            vF_006->vF_007
            vF_007->end
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
            vF_000 [label=<De&#9251;vent&#9251;was&#9251;woedend&#9251;en&#9251;maakte&#9251;<br/><i>F: /text/s</i>>]
            vF_006 [label=<Shiriar<br/><i>F: /text/s/app/rdg/del</i>>]
            vF_007 [label=<&#9251;den&#9251;bedremmelden&#9251;Sultan&#9251;uit&#x21A9;<br/>&#9251;voor&#9251;"lompen&#9251;boer".<br/><i>F: /text/s</i>>]
            end [label="";shape=doublecircle,rank=middle]
            begin->vF_000
            vF_000->vF_006
            vF_006->vF_007
            vF_007->end
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
            vQ_000 [label=<De&#9251;vent&#9251;was&#9251;woedend&#9251;en&#9251;maakte&#9251;<br/><i>Q: /text/s</i>>]
            vQ_006 [label=<Shiriar<br/><i>Q: /text/s/app/rdg[@type='l1']</i>>]
            vQ_007 [label=<den&#9251;bedremmelden&#9251;man<br/><i>Q: /text/s/app/rdg[@type='l2']</i>>]
            vQ_010 [label=<den&#9251;bedremmelden&#9251;Sultan<br/><i>Q: /text/s/app/rdg[@type='l3']</i>>]
            vQ_013 [label=<uit&#9251;voor&#9251;<br/><i>Q: /text/s</i>>]
            vQ_015 [label=<"lompen&#9251;boer"<br/><i>Q: /text/s/q</i>>]
            vQ_017 [label=<.<br/><i>Q: /text/s</i>>]
            end [label="";shape=doublecircle,rank=middle]
            begin->vQ_000
            vQ_000->vQ_006
            vQ_000->vQ_007
            vQ_000->vQ_010
            vQ_006->vQ_013
            vQ_007->vQ_013
            vQ_010->vQ_013
            vQ_013->vQ_015
            vQ_015->vQ_017
            vQ_017->end
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
            vQ_000 [label=<&#9251;<br/><i>Q: /text</i>>]
            vQ_001 [label=<De&#9251;vent&#9251;was&#9251;woedend&#9251;en&#9251;maakte&#9251;<br/><i>Q: /text/s</i>>]
            vQ_007 [label=<Shiriar<br/><i>Q: /text/s/app/rdg/del</i>>]
            vQ_008 [label=<den&#9251;bedremmelden&#9251;&#x21A9;<br/>&#9251;<br/><i>Q: /text/s/app/rdg/add</i>>]
            vQ_012 [label=<&#9251;uit&#9251;voor&#9251;<br/><i>Q: /text/s</i>>]
            vQ_010 [label=<man<br/><i>Q: /text/s/app/rdg/add/app/rdg/del</i>>]
            vQ_011 [label=<Sultan<br/><i>Q: /text/s/app/rdg/add/app/rdg/add</i>>]
            vQ_015 [label=<"lompen&#9251;boer"<br/><i>Q: /text/s/q</i>>]
            vQ_017 [label=<.<br/><i>Q: /text/s</i>>]
            end [label="";shape=doublecircle,rank=middle]
            begin->vQ_000
            vQ_000->vQ_001
            vQ_001->vQ_007
            vQ_001->vQ_008
            vQ_007->vQ_012
            vQ_008->vQ_010
            vQ_008->vQ_011
            vQ_010->vQ_012
            vQ_011->vQ_012
            vQ_012->vQ_015
            vQ_015->vQ_017
            vQ_017->end
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
            vF_000 [label=<Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/><i>F: /text/s</i>>]
            vF_006 [label=<<br/><i>F: /text/s/lb</i>>]
            vF_007 [label=<werven&#9251;om<br/><i>F: /text/s/app/rdg[@type='l1']</i>>]
            vF_009 [label=<trachten&#9251;naar<br/><i>F: /text/s/app/rdg[@type='l2']</i>>]
            vF_011 [label=<&#9251;een&#9251;vrouw,&#9251;de&#9251;ongewisheid&#9251;vóór&#9251;de&#9251;<br/><i>F: /text/s</i>>]
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
            <rdg varSeq="1"><del>werven om</del></rdg>
            <rdg varSeq="2"><add>trachten naar</add></rdg>
            <rdg type="lit"><hi rend="strike">werven om</hi> <hi rend="supralinear">trachten naar</hi></rdg>
        </app> 
        een vrouw, de ongewisheid vóór de <lb/>liefelijke toestemming!</s>
</text>""")
        val expectedDot = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            vF_000 [label=<Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/><i>F: /text/s</i>>]
            vF_006 [label=<<br/><i>F: /text/s/lb</i>>]
            vF_007 [label=<werven&#9251;om<br/><i>F: /text/s/app/rdg[@varSeq='1']/del</i>>]
            vF_009 [label=<trachten&#9251;naar<br/><i>F: /text/s/app/rdg[@varSeq='2']/add</i>>]
            vF_011 [label=<&#9251;een&#9251;vrouw,&#9251;de&#9251;ongewisheid&#9251;vóór&#9251;de&#9251;<br/><i>F: /text/s</i>>]
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
            vF_000 [label=<Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/><i>F: /text/s</i>>]
            vF_006 [label=<werven&#9251;om<br/><i>F: /text/s/app/rdg[@type='l1']</i>>]
            vF_008 [label=<trachten&#9251;naar<br/><i>F: /text/s/app/rdg[@type='l2']</i>>]
            vF_010 [label=<&#9251;een&#9251;<br/><i>F: /text/s</i>>]
            vF_012 [label=<<br/><i>F: /text/s/lb</i>>]
            vF_013 [label=<vrouw&#9251;!<br/><i>F: /text/s</i>>]
            vF_015 [label=<Die&#9251;dagen&#9251;van&#9251;nerveuze&#9251;verwachting&#9251;vóór&#9251;de&#9251;liefelijke&#9251;toestemming.<br/><i>F: /text/s</i>>]
            end [label="";shape=doublecircle,rank=middle]
            begin->vF_000
            vF_000->vF_006
            vF_000->vF_008
            vF_006->vF_010
            vF_008->vF_010
            vF_010->vF_012
            vF_012->vF_013
            vF_013->vF_015
            vF_015->end
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
            vF_000 [label=<Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/><i>F: /text/s</i>>]
            vF_006 [label=<werven&#9251;om<br/><i>F: /text/s/app/rdg/del</i>>]
            vF_008 [label=<trachten&#9251;naar<br/><i>F: /text/s/app/rdg/add</i>>]
            vF_010 [label=<&#9251;een&#9251;<br/><i>F: /text/s</i>>]
            vF_012 [label=<<br/><i>F: /text/s/lb</i>>]
            vF_013 [label=<vrouw&#9251;!<br/><i>F: /text/s</i>>]
            vF_015 [label=<Die&#9251;dagen&#9251;van&#9251;nerveuze&#9251;verwachting&#9251;vóór&#9251;de&#9251;liefelijke&#9251;toestemming.<br/><i>F: /text/s</i>>]
            end [label="";shape=doublecircle,rank=middle]
            begin->vF_000
            vF_000->vF_006
            vF_000->vF_008
            vF_006->vF_010
            vF_008->vF_010
            vF_010->vF_012
            vF_012->vF_013
            vF_013->vF_015
            vF_015->end
            }
            """.trimIndent()
        verifyDotExport(wg0, expectedDot, "witness-q-hierarchy-app-rdg-vincent")
    }

    @Test
    fun testSingleAdd() {
        val importer = XMLImporter()
        val a = importer.importXML(
                "1", "<p><s>&amp; himself corrected <add>and augmented</add> them</s></p>")
        val markups = a.markupList
        assertThat(markups).hasSize(3)
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
            v1_000 [label=<&amp;>]
            v1_001 [label=<&#9251;>]
            v1_002 [label=<himself&#9251;>]
            v1_003 [label=<corrected&#9251;>]
            subgraph cluster_2 {
            label=<<i><b>add</b></i>>
            graph[style="rounded,filled";fillcolor="#9aed7d"]
            v1_004 [label=<and&#9251;>]
            v1_005 [label=<augmented>]
            }
            v1_006 [label=<&#9251;>]
            v1_007 [label=<them>]
            }
            }
            end [label="";shape=doublecircle,rank=middle]
            begin->v1_000
            v1_000->v1_001
            v1_001->v1_002
            v1_002->v1_003
            v1_003->v1_004
            v1_003->v1_006
            v1_004->v1_005
            v1_005->v1_006
            v1_006->v1_007
            v1_007->end
            }
            """.trimIndent()
        assertThat(dot).isEqualTo(expectedDot)
    }

    @Test
    fun normalize_siglum() {
        val softly = SoftAssertions()
        with(softly) {
            assertThat("A".normalizedSiglum()).isEqualTo("A")
            assertThat("remove space".normalizedSiglum()).isEqualTo("removespace")
            assertThat("1 2 3 4".normalizedSiglum()).isEqualTo("1234")
            assertAll()
        }
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

