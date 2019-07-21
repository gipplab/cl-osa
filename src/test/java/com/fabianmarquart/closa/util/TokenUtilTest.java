package com.fabianmarquart.closa.util;

import com.fabianmarquart.closa.model.Token;
import edu.stanford.nlp.simple.Document;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Test class for TokenUtil.
 * <p>
 * Created by Fabian Marquart on 2017/03/27.
 */
public class TokenUtilTest {

    @Test
    public void testTokenization() {
        String text = "Paragraph 1 \n \n This is an example (text). Next sentence. \n \n" +
                "Paragraph 2 \n \n This text is the second paragraph.";

        List<Token> tokens = TokenUtil.tokenize(text, false);
        Assert.assertTrue(tokens.size() == 7);
    }

    @Test
    public void testNamedEntityTokenize() {
        String text = "Joe Smith was born in California. " +
                "In 2017, he went to Paris, France in the summer. " +
                "His flight left at 3:00pm on July 10th, 2017. " +
                "After eating some escargot for the first time, Joe said, \"That was delicious!\" " +
                "He sent a postcard to his sister Jane Smith. " +
                "After hearing about Joe's trip, Jane decided she might go to France one day.";

        List<List<Token>> tokensBySentence = TokenUtil.namedEntityTokenize(text, "en");
        System.out.println(tokensBySentence);

        Assert.assertTrue(tokensBySentence.get(4).contains(new Token("Jane Smith", "Jane Smith", "NNP", Token.NamedEntityType.PERSON)));
        Assert.assertTrue(tokensBySentence.get(5).contains(new Token("France", "France", "NNP", Token.NamedEntityType.LOCATION)));
    }

    @Test
    public void testNamedEntityTokenize2() {
        String text = "A German man has been charged with incitement to hatred after he was pictured with a tattoo apparently of the Nazi death camp at Auschwitz.";

        List<List<Token>> tokensBySentence = TokenUtil.namedEntityTokenize(text, "en");
        System.out.println(tokensBySentence);

        Assert.assertTrue(tokensBySentence.get(1).contains(new Token("Auschwitz", "Auschwitz", "NNP", Token.NamedEntityType.LOCATION)));

    }

    @Test
    public void testNamedEntityTokenizeJapanese() {
        String text = "ドイツ人男性が、ナチスドイツの強制収容所を描いたとされるタトゥーをプールでさらしたとして、憎悪扇動の罪で起訴された。";

        List<List<Token>> tokensBySentence = TokenUtil.namedEntityTokenize(text, "ja");
        System.out.println(tokensBySentence);
    }

    @Test
    public void testIsLatinAlphabet() {
        String testStringLatin = "Philip Falcone";
        String testStringPartialLatin = "菲利普•法尔科(Philip Falcone)";
        String testStringNoLatin = "菲利普•法尔科";

        Assert.assertTrue(TokenUtil.isLatinAlphabet(testStringLatin));
        Assert.assertFalse(TokenUtil.isLatinAlphabet(testStringPartialLatin));
        Assert.assertFalse(TokenUtil.isLatinAlphabet(testStringNoLatin));
    }

    @Test
    public void testStemming() {
        String textEn = "english words don't have many possibilities to be stemmed 70s";
        String textDe = "Deutsche Wörter, vor allem beschreibende Adjektive, sind viel komplizierter";

        List<Token> tokensEn = TokenUtil.tokenize(textEn, true);
        tokensEn = TokenUtil.stem(tokensEn, "en");
        List<Token> tokensDe = TokenUtil.tokenize(textDe, true);

        Assert.assertTrue(tokensEn.contains(new Token("mani")));
        Assert.assertTrue(tokensEn.contains(new Token("1970s")));
        Assert.assertTrue(TokenUtil.stem(tokensDe, "de").contains(new Token("beschreib")));
    }

    @Test
    public void testPreprocessing() {
        String text = "and and or he she it test 2";

        List<Token> tokens = TokenUtil.tokenize(text, "en");
        tokens = TokenUtil.removeStopwords(tokens, "en");
        Assert.assertEquals(tokens.size(), 2);

        tokens = TokenUtil.removeNumbers(tokens);
        Assert.assertTrue(!tokens.contains(new Token("2")));
    }

    @Test
    public void testNounPhraseDiscrimination() {

        List<Token> germanTokens = TokenUtil.tokenize("Soft-Decision-Fusion Soft-Decision-Fusionsalgorithmen kombinieren Ansätze aus der " +
                "Early-Signal -Fusion und der Late-Semantic-Fusion. Es werden nicht nur die Konfidenzmaße " +
                "des eigentlichen Erkennerergebnisses in die Fusion mit einbezogen, sondern auch N-best-Listen " +
                "aus jedem Erkenner. \n" +
                "\n" +
                "[So kann sichergestellt werden, dass bei der Fusion nicht unbedingt immer das " +
                "Einzelerkennerergebnis mit der höchsten Konfidenz verwendet wird, sondern das Ergebnis, " +
                "das zur höchsten Gesamtkonfidenz beiträgt.] ", "de");

        germanTokens = TokenUtil.keepNounPhrases(germanTokens, "de");

        Assert.assertTrue(germanTokens.contains(new Token("Konfidenzmaß")));
        Assert.assertTrue(!germanTokens.contains(new Token("aus")));

        List<Token> englishTokens = TokenUtil.tokenize("This is a noun, this is a modified noun", "en");
        englishTokens = TokenUtil.keepNounPhrases(englishTokens, "en");
        List<Token> testTokens = TokenUtil.tokenize("noun noun", "en");

        Assert.assertEquals(englishTokens, testTokens);
    }

    @Test
    public void testLemmatization() {
        String input = "Asian";

        List<Token> output = TokenUtil.namedEntityTokenize(input, "en")
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        System.out.println(output);
    }

    @Test
    public void testComplexLemmatization() {

        String input = "das zentrale Nervensystem, der dekadische Logarithmus, ein dekadischer Logarithmus, " +
                " die erzwungene Schwingung," +
                " die erzwungenen Schwingungen, elektrodynamische Lautsprecher";

        List<Token> output = TokenUtil.germanLemmatize(TokenUtil.tokenize(input, "de"));

        Assert.assertTrue(output.contains(new Token("dekadischer")));
        Assert.assertTrue(output.contains(new Token("zentrales")));

        String input2 = "Aktivierungs- und Relaxationslösung\n" +
                "Um den isolierten kontraktilen Apparat des Myokards zu aktivieren bzw. wieder zu relaxieren, " +
                "wurden Lösungen mit unterschiedlichen Calciumkonzentrationen verwendet:\n" +
                "\n" +
                "Als Relaxationslösung wurde verwendet: 10 mM ATP, 12.5 mM MgCl2, 5 mM EGTA, 20 mM Imidazol, " +
                "5 mM NaN3, 1O mM Phosphocreatin, 400 U/ml CPK.\n" +
                "\n" +
                "Die Aktivierungslösung entspricht in ihrer Zusammensetzung der Relaxationslösung, wobei das " +
                "EGTA durch entsprechende Konzentrationen CaEGTA ersetzt wurde. Die Calciumionenkonzentration " +
                "in der Badlösung wurde als negativer dekadischer Logarithmus angegeben (pCa). Eine freie " +
                "Calciumionenkonzentration von 10 nmol entspricht damit einem berechneten pCa von 8, die " +
                "supramaximale Aktivierungslösung einem pCa von 4.3. Die Berechnungen erfolgten nach der Formel " +
                "von Fabiato und Fabiato (1979).";

        List<Token> output2 = TokenUtil.germanLemmatize(TokenUtil.tokenize(input2, "de"));

        Assert.assertTrue(output2.contains(new Token("unterschiedliche")));
    }

    @Test
    public void testGermanTokenization() {
        String text = "Aktivierungs- und Relaxationslösung\n" +
                "Um den isolierten kontraktilen Apparat des Myokards zu aktivieren bzw. wieder zu relaxieren," +
                "wurden Lösungen mit unterschiedlichen Calciumkonzentrationen verwendet:\n" +
                "\n" +
                "Als Relaxationslösung wurde verwendet: 10 mM ATP, 12.5 mM MgCl2, 5 mM EGTA, 20 mM Imidazol," +
                "5 mM NaN3, 1O mM Phosphocreatin, 400 U/ml CPK.\n" +
                "\n" +
                "Die Aktivierungslösung entspricht in ihrer Zusammensetzung der Relaxationslösung, wobei das" +
                "EGTA durch entsprechende Konzentrationen CaEGTA ersetzt wurde. Die Calciumionenkonzentration" +
                "in der Badlösung wurde als negativer dekadischer Logarithmus angegeben (pCa). Eine freie" +
                "Calciumionenkonzentration von 10 nmol entspricht damit einem berechneten pCa von 8, die" +
                "supramaximale Aktivierungslösung einem pCa von 4.3. Die Berechnungen erfolgten nach der" +
                "Formel von Fabiato und Fabiato (1979).";

        edu.stanford.nlp.simple.Document doc = new Document(text);
        Assert.assertEquals(doc.sentence(0).posTags().get(0), "SYM");

        List<Token> tokens = TokenUtil.tokenize(text, true);
        Assert.assertEquals(tokens.size(), 2);
    }


    @Test
    public void testJapaneseTokenization() {
        String text = "私はバカです。宜しくお願い致します。";

        List<Token> tokens = TokenUtil.tokenize(text, true);

        Assert.assertTrue(tokens.contains(new Token("私")));

        String textWithNewlines = "盗作（とうさく）は、他人の著作物にある表現、その他独自性・" +
                "独創性のあるアイディア・企画等を盗用し、それを独自に考え出したものとして公衆に提示する反倫理的な行為全般を指す。" +
                "「剽窃（ひょうせつ）」とも呼ばれる。オマージュ、パロディとは区別される。\n" +
                "盗作は学業不正及び報道倫理の侵犯と見做され、それに対しては罰金、停職、追放などの処分が行われる。" +
                "盗作は必ずしも犯罪とはならないが、学業や産業の分野においては深刻な倫理、道義違反とされる。\n" +
                "学業の分野においては、学生、研究者、調査者によって行われた剽窃は学業不正及び学問に対する欺瞞と見做され、" +
                "譴責の対象となり、その後追放を含めた処分が行われる。";

        List<Token> tokensWithNewlines = TokenUtil.tokenize(textWithNewlines, true);

        Assert.assertEquals(tokensWithNewlines.size(), 3);
    }

    @Test
    public void testChineseTokenization() {
        String text = "克林顿说，华盛顿将逐步落实对韩国的经济援助。"
                + "金大中对克林顿的讲话报以掌声：克林顿总统在会谈中重申，他坚定地支持韩国摆脱经济危机。";

        List<Token> tokens = TokenUtil.tokenize(text, true);

        Assert.assertEquals(tokens.size(), 38);
    }

    @Test
    public void testNGramPartition() {
        String text = "Aktivierungslösung";
        List<String> nGrams = TokenUtil.nGramPartition(text, 3);
        Assert.assertTrue(nGrams.size() == 16);
    }

    @Test
    public void testRemovePunctuation() {
        List<String> tokens = Arrays.asList("。", "教授", "言語", "フランス語", "アラビア", "語", "両方", "なっ", "おり",
                "、", "大", "多数", "国民", "フランス語", "話す", "こと", "可能", "。", "アラビア", "語", "チュニジア", "方言",
                "マルタ", "語", "近い", "。", "また", "、", "ごく", "少数", "ながら", "ベルベル", "語", "一つ", "シェルハ",
                "話さ", "いる", "。", " ");

        List<String> filteredTokens = TokenUtil.removePunctuation(tokens.stream().map(Token::new).collect(Collectors.toList()))
                .stream().map(Token::getToken).collect(Collectors.toList());

        Assert.assertFalse(filteredTokens.contains("。"));
        Assert.assertFalse(filteredTokens.contains("、"));
    }

    @Test
    public void testGetPunctuation() {
        List<String> symbols = TokenUtil.getPunctuation();

        Assert.assertFalse(symbols.isEmpty());
    }
}