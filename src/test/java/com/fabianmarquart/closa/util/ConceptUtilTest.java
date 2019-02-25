package com.fabianmarquart.closa.util;

import com.fabianmarquart.closa.model.Token;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

/**
 * Created by Fabian Marquart on 2018/02/23
 */
public class ConceptUtilTest {

    @Test
    public void testGetPageTitleMapTest() {
        String englishText = "bread|butter";
        String japaneseText = "パン|お好み焼き";


        Map<String, String> pageTitleMapEn = ConceptUtil.getPageTitleMap(englishText, "en");
        Map<String, String> pageTitleMapJa = ConceptUtil.getPageTitleMap(japaneseText, "ja");

        Assert.assertEquals(pageTitleMapEn.get("bread"), "present");
        Assert.assertEquals(pageTitleMapJa.get("パン"), "present");
    }

    @Test
    public void testConceptDetectionTest() {
        String englishText = "Plagiarism is the \"wrongful appropriation\" and \"stealing and publication\" of " +
                "another author's \"language, thoughts, ideas, or expressions\" and the representation of " +
                "them as one's own original work.";

        String japaneseText = "盗作（とうさく）は、他人の著作物にある表現、その他独自性・" +
                "独創性のあるアイディア・企画等を盗用し、それを独自に考え出したものとして公衆に提示する反倫理的な行為全般を指す。" +
                "「剽窃（ひょうせつ）」とも呼ばれる。オマージュ、パロディとは区別される。\n" +
                "盗作は学業不正及び報道倫理の侵犯と見做され、それに対しては罰金、停職、追放などの処分が行われる。" +
                "盗作は必ずしも犯罪とはならないが、学業や産業の分野においては深刻な倫理、道義違反とされる。\n" +
                "学業の分野においては、学生、研究者、調査者によって行われた剽窃は学業不正及び学問に対する欺瞞と見做され、" +
                "譴責の対象となり、その後追放を含めた処分が行われる。";

        List<Token> tokensEn = TokenUtil.tokenize(englishText, "en");
        List<Token> conceptsEn = ConceptUtil.getConceptsFromTokens(tokensEn, 3, "en");
        Assert.assertEquals(conceptsEn.get(0), new Token("plagiarism"));
        Assert.assertTrue(!conceptsEn.contains(new Token("and")));

        List<Token> tokensJa = TokenUtil.tokenize(japaneseText, "ja");
        List<Token> conceptsJa = ConceptUtil.getConceptsFromTokens(tokensJa, 3, "ja");
        Assert.assertEquals(conceptsJa.get(0), new Token("盗作"));
    }

    @Test
    public void testGetPageIdInLanguage() {
        String anarchismPageIdEnglish = "12";

        System.out.println(ConceptUtil.getPageIdInLanguage(anarchismPageIdEnglish, "en", "ja"));

        String geographyPageIdJapanese = "12";

        System.out.println(ConceptUtil.getPageIdInLanguage(geographyPageIdJapanese, "ja", "en"));
    }

    @Test
    public void testGetPageIdInLanguageWithoutLink() {
        String patriotWhigsId = "4227252";
        String pageIdInLanguage = ConceptUtil.getPageIdInLanguage(patriotWhigsId, "en", "de");

        Assert.assertTrue(pageIdInLanguage == null);
    }

    @Test
    public void testGetPageSummary() {
        String stackOverflow = "21721040";

        System.out.println(ConceptUtil.getPageContent(stackOverflow, "en"));

        System.out.println(ConceptUtil.getPageContent(ConceptUtil.getPageIdInLanguage(stackOverflow, "en", "ja"), "ja"));
    }
}
