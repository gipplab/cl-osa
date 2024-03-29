package com.iandadesign.closa.util;

import com.iandadesign.closa.model.Token;
import edu.stanford.nlp.simple.Document;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    void namedEntityTokenize() {
        String text = "Joe Smith was born in California. " +
                "In 2017, he went to Paris, France in the summer. " +
                "His flight left at 3:00pm on July 10th, 2017. " +
                "After eating some escargot for the first time, Joe said, \"That was delicious!\" " +
                "He sent a postcard to his sister Jane Smith. " +
                "After hearing about Joe's trip, Jane decided she might go to France one day.";

        List<List<Token>> tokensBySentence = TokenUtil.namedEntityTokenize(text, "en");

        Assertions.assertTrue(tokensBySentence.get(4).contains(new Token("Jane Smith", "Jane Smith", "NNP", Token.NamedEntityType.PERSON)));
        Assertions.assertTrue(tokensBySentence.get(5).contains(new Token("France", "France", "NNP", Token.NamedEntityType.LOCATION)));

        text = "A German man has been charged with incitement to hatred after he was pictured with a tattoo apparently of the Nazi death camp at Auschwitz.";

        tokensBySentence = TokenUtil.namedEntityTokenize(text, "en");

        Assertions.assertTrue(tokensBySentence.get(0).contains(new Token("Auschwitz", "Auschwitz", "NNP", Token.NamedEntityType.LOCATION)));
    }

    @Test
    void tokenize() {
        String text = "Paragraph 1 \n \n This is an example (text). Next sentence. \n \n" +
                "Paragraph 2 \n \n This text is the second paragraph.";

        List<Token> tokens = TokenUtil.tokenize(text, false);
        Assertions.assertEquals(17, tokens.size());
    }

    @Test
    void tokenizeLowercaseStemAndRemoveStopwords() {
        String text = "Paragraph 1 \n \n This is an example (text). Next sentence. \n \n" +
                "Paragraph 2 \n \n This text is the second paragraph.";

        List<Token> tokens = TokenUtil.tokenizeLowercaseStemAndRemoveStopwords(text, "en");
        Assertions.assertEquals(10, tokens.size());
    }


    @Test
    void germanLemmatize() {
        String input = "das zentrale Nervensystem, der dekadische Logarithmus, ein dekadischer Logarithmus, " +
                " die erzwungene Schwingung," +
                " die erzwungenen Schwingungen, elektrodynamische Lautsprecher";

        List<Token> output = TokenUtil.germanLemmatize(TokenUtil.tokenize(input, "de"));

        Assertions.assertTrue(output.contains(new Token("dekadischer")));
        Assertions.assertTrue(output.contains(new Token("zentrales")));

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

        Assertions.assertTrue(output2.contains(new Token("unterschiedliche")));
    }

    @Test
    void stem() {
        String textEn = "english words don't have many possibilities to be stemmed 70s";
        String textDe = "Deutsche Wörter, vor allem beschreibende Adjektive, sind viel komplizierter";

        List<Token> tokensEn = TokenUtil.tokenize(textEn, true);
        tokensEn = TokenUtil.stem(tokensEn, "en");
        List<Token> tokensDe = TokenUtil.tokenize(textDe, true);

        Assertions.assertTrue(tokensEn.contains(new Token("mani")));
        Assertions.assertTrue(tokensEn.contains(new Token("1970s")));
        Assertions.assertTrue(TokenUtil.stem(tokensDe, "de").contains(new Token("beschreib")));
    }

    @Test
    void keepNounPhrases() {
        List<Token> germanTokens = TokenUtil.tokenize("Soft-Decision-Fusion Soft-Decision-Fusionsalgorithmen kombinieren Ansätze aus der " +
                "Early-Signal -Fusion und der Late-Semantic-Fusion. Es werden nicht nur die Konfidenzmaße " +
                "des eigentlichen Erkennerergebnisses in die Fusion mit einbezogen, sondern auch N-best-Listen " +
                "aus jedem Erkenner. \n" +
                "\n" +
                "[So kann sichergestellt werden, dass bei der Fusion nicht unbedingt immer das " +
                "Einzelerkennerergebnis mit der höchsten Konfidenz verwendet wird, sondern das Ergebnis, " +
                "das zur höchsten Gesamtkonfidenz beiträgt.] ", "de");

        germanTokens = TokenUtil.keepNounPhrases(germanTokens, "de");

        Assertions.assertTrue(germanTokens.contains(new Token("Konfidenzmaß")));
        Assertions.assertFalse(germanTokens.contains(new Token("aus")));

        List<Token> englishTokens = TokenUtil.tokenize("This is a noun, this is a modified noun", "en");
        englishTokens = TokenUtil.keepNounPhrases(englishTokens, "en");
        List<Token> testTokens = TokenUtil.tokenize("noun noun", "en");

        Assertions.assertEquals(englishTokens, testTokens);
    }

    @Test
    void removeNumbers() {
        String text = "test 2";
        List<Token> tokens = TokenUtil.tokenize(text, "en");

        tokens = TokenUtil.removeNumbers(tokens);
        Assertions.assertFalse(tokens.contains(new Token("2")));
    }

    @Test
    void getPunctuation() {
        List<String> symbols = TokenUtil.getPunctuation();
        Assertions.assertFalse(symbols.isEmpty());
    }

    @Test
    void removePunctuation() {
        List<String> tokens = Arrays.asList("。", "教授", "言語", "フランス語", "アラビア", "語", "両方", "なっ", "おり",
                "、", "大", "多数", "国民", "フランス語", "話す", "こと", "可能", "。", "アラビア", "語", "チュニジア", "方言",
                "マルタ", "語", "近い", "。", "また", "、", "ごく", "少数", "ながら", "ベルベル", "語", "一つ", "シェルハ",
                "話さ", "いる", "。", " ");

        List<String> filteredTokens = TokenUtil.removePunctuation(tokens.stream().map(Token::new).collect(Collectors.toList()))
                .stream().map(Token::getToken).collect(Collectors.toList());

        Assertions.assertFalse(filteredTokens.contains("。"));
        Assertions.assertFalse(filteredTokens.contains("、"));
    }

    @Test
    void getStopwords() {
        List<String> languages = Arrays.asList("de", "en", "es", "fr", "ja", "zh");

        for (String language : languages) {
            List<String> stopwords = TokenUtil.getStopwords(language);
            Assertions.assertFalse(stopwords.isEmpty());
        }
    }

    @Test
    void removeStopwords() {
        String text = "and and or he she it test 2";

        List<Token> tokens = TokenUtil.tokenize(text, "en");
        tokens = TokenUtil.removeStopwords(tokens, "en");
        Assertions.assertEquals(tokens.size(), 2);
    }

    @Test
    void nGramPartition() {
        String text = "Aktivierungslösung";
        List<String> nGrams = TokenUtil.nGramPartition(text, 3);
        Assertions.assertEquals(16, nGrams.size());
    }


    @Test
    void isLatinAlphabet() {
        String testStringLatin = "Philip Falcone";
        String testStringPartialLatin = "菲利普•法尔科(Philip Falcone)";
        String testStringNoLatin = "菲利普•法尔科";

        Assertions.assertTrue(TokenUtil.isLatinAlphabet(testStringLatin));
        Assertions.assertFalse(TokenUtil.isLatinAlphabet(testStringPartialLatin));
        Assertions.assertFalse(TokenUtil.isLatinAlphabet(testStringNoLatin));
    }

    @Test
    public void namedEntityTokenizeJapanese() {
        String text = "ドイツ人男性が、ナチスドイツの強制収容所を描いたとされるタトゥーをプールでさらしたとして、憎悪扇動の罪で起訴された。";

        List<List<Token>> tokensBySentence = TokenUtil.namedEntityTokenize(text, "ja");
    }


    @Test
    public void germanTokenization() {
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
        Assertions.assertEquals(doc.sentence(0).posTags().get(0), "SYM");

        List<Token> tokens = TokenUtil.tokenize(text, true);
        Assertions.assertEquals(tokens.size(), 102);
    }


    @Test
    public void japaneseTokenize() {
        String text = "私はバカです。宜しくお願い致します。";

        List<Token> tokens = TokenUtil.tokenize(text, true);

        Assertions.assertTrue(tokens.contains(new Token("私")));

        String textWithNewlines = "盗作（とうさく）は、他人の著作物にある表現、その他独自性・" +
                "独創性のあるアイディア・企画等を盗用し、それを独自に考え出したものとして公衆に提示する反倫理的な行為全般を指す。" +
                "「剽窃（ひょうせつ）」とも呼ばれる。オマージュ、パロディとは区別される。\n" +
                "盗作は学業不正及び報道倫理の侵犯と見做され、それに対しては罰金、停職、追放などの処分が行われる。" +
                "盗作は必ずしも犯罪とはならないが、学業や産業の分野においては深刻な倫理、道義違反とされる。\n" +
                "学業の分野においては、学生、研究者、調査者によって行われた剽窃は学業不正及び学問に対する欺瞞と見做され、" +
                "譴責の対象となり、その後追放を含めた処分が行われる。";

        List<Token> tokensWithNewlines = TokenUtil.tokenize(textWithNewlines, true);

        Assertions.assertEquals(tokensWithNewlines.size(), 178);
    }

    @Test
    public void chineseTokenize() {
        String text = "克林顿说，华盛顿将逐步落实对韩国的经济援助。"
                + "金大中对克林顿的讲话报以掌声：克林顿总统在会谈中重申，他坚定地支持韩国摆脱经济危机。";

        List<Token> tokens = TokenUtil.tokenize(text, true);

        Assertions.assertEquals(tokens.size(), 32);
    }


    @Test
    void chineseTokenizeTEDCorpus() {
        String path = "/Users/fabianmarquart/TED_Paracorpus/TED.ZH.txt";
        String pathTokenized = "/Users/fabianmarquart/TED_Paracorpus/TED.ZH-tokenized.txt";

        try {
            List<String> lines = FileUtils.readLines(new File(path), StandardCharsets.UTF_8);
            List<String> outputLines = lines.stream()
                    .map((String line) -> {
                        try {
                            return TokenUtil.chineseTokenize(line, "zh")
                                    .stream()
                                    .map(Token::getToken)
                                    .collect(Collectors.joining(" "));
                        } catch (IndexOutOfBoundsException e) {
                            e.printStackTrace();
                            return "";
                        }
                    })
                    .collect(Collectors.toList());

            FileUtils.writeLines(new File(pathTokenized), outputLines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void japaneseTokenizeTanakaCorpus() {
        String path = "/Users/fabianmarquart/tanaka-corpus/ja.utf";
        String pathTokens = "/Users/fabianmarquart/tanaka-corpus/ja-tokenized.utf";

        try {
            List<String> lines = FileUtils.readLines(new File(path), StandardCharsets.UTF_8);
            List<String> outputLines = lines.stream()
                    .map((String line) -> {
                        // System.out.println(line);
                        try {
                            return TokenUtil.tokenize(line, "ja")
                                    .stream()
                                    .map(Token::getToken)
                                    .collect(Collectors.joining(" "));
                        } catch (IndexOutOfBoundsException e) {
                            e.printStackTrace();
                            return "";
                        }
                    })
                    .collect(Collectors.toList());

            FileUtils.writeLines(new File(pathTokens), outputLines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}