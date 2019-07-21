package com.iandadesign.closa.classification;

import com.iandadesign.closa.util.TokenUtil;
import de.daslaboratorium.machinelearning.classifier.bayes.BayesClassifier;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class TextClassifierUtilTest {


    /**
     * Simple test for the bayesian classifier.
     */
    @Test
    public void testClassify() {
        TextClassifier textClassifier = new TextClassifier();

        String englishText =
                "2.1.4 virulence factors virulence factors are defined as bacterial products that cause direct " +
                        "damage to the host cells. Virulence-associated factors enable the growth or Survival of the pathogen " +
                        "in the host and in this way contribute to infection and illness (MAHAN et al. 1996";

        String germanText =
                "SD kann im empfindlichen Bereich durch eine einzelne Entladung eines epileptischen Fokus " +
                        "ausgelöst werden (spike triggered SD). In der Regel werden epileptiforme Feldpotentiale während der SD " +
                        "unterdrückt und erscheinen in wenigen Minuten wieder.";

        String japaneseText =
                "ドイツ西部ケルンで大みそかに若者の集団が性的暴行や窃盗に及んだ事件を受けて5日、女性を中心に数百人が" +
                        "ケルンで抗議デモを行った。独国民に衝撃を与えたこの事件についてメルケル独首相は「胸が悪くなるような襲撃」と怒りを表明し、" +
                        "犯人を捕らえるためあらゆる手を尽くす必要があると強調した。\n" +
                        "目撃者や警察は、事件を起こした若者たちはアラブ系か北アフリカ出身のように見えたと証言している。\n" +
                        "ドイツでは、昨年に記録的な数で流入した移民や難民の受け入れをめぐり激しい議論が交わされている。移民の多くはシリア内戦" +
                        "を逃れてきた人々だ。\n" +
                        "ケルンのヘンリエッテ・レーカー市長は、犯人について早急な結論に飛びつくべきでないと訴えた。現時点では逮捕者は出ていない。";

        String chineseText =
                "二十年前的今天，当人们推倒柏林墙时，在欢庆自由的混乱人群中，很少有人能预见到这件事的后果。\n" +
                        "但抓住了这一时刻，他们便创造了历史。\n" +
                        "和那座混凝土的砖石建筑一样，德国及欧洲始于1945年的分裂状态也从此消失，由此打开了通向冷战结束、民主和自由市场进入东欧、以及欧盟(EU)壮大的道路。\n" +
                        "二十年过去了，很明显，世界从共产主义的倒台中获得了巨大的政治与经济利益。\n" +
                        "尽管今天还存在全球恐怖主义、中东问题和经济危机这样的难题，但超级大国对立局面的结束，让世界变得更加安全、更加自由、也更加富裕。\n" +
                        "德国与东欧经济一体化的完成依然存在困难，上述地区的生活水平仍然低于西方。";

        String spanishText =
                "Nada puede ser tan interesante para los pueblos del Nuevo Mundo como el estudio social de España.\n" +
                        "Las sociedades de una y otra region son muy homogéneas en sus condiciones esenciales,--en su\n" +
                        "educacion sobre todo,--y conviene compararlas para que se vea que donde quiera las mismas causas,\n" +
                        "es decir, las mismas instituciones, producen resultados análogos, habida consideracion á las\n" +
                        "diferencias geográficas. El rápido viaje de tres meses que pude hacer en España es, sin duda\n" +
                        "ninguna, muy insuficiente para formular apreciaciones bastante sólidas, tanto mas cuanto que\n" +
                        "España es uno de los pueblos que tienen ménos unidad etnográfica y social entre los mas notables\n" +
                        "de Europa.";

        String frenchText =
                "Madame la Présidente, permettez-moi de rappeler que demain aura lieu le deuxième anniversaire de la tragédie du Cermès.\n" +
                        "        En effet, il y a deux ans, à Cavalese en Italie, un avion américain venant de la base " +
                        "de l'OTAN situé à Aviano, trancha, lors d'un exercice à basse altitude - en deçà des limites de " +
                        "sécurité -, les câbles d'un téléphérique, causant la mort de plus de vingt citoyens européens.";

        Assert.assertEquals(textClassifier.classifyText(englishText, "en"), Category.biology);
        Assert.assertEquals(textClassifier.classifyText(germanText, "de"), Category.biology);
    }


    /**
     * Preliminary tests for the bayes classifier.
     */
    @Test
    public void testSimpleBayes() {
        // Create a new bayes classifier with string categories and string features.
        BayesClassifier<String, String> bayes = new BayesClassifier<>();
        bayes.setMemoryCapacity(Integer.MAX_VALUE);

        // Two examples to learn from.
        List<String> positiveText = TokenUtil.nGramPartition("I love sunny days", 3);
        List<String> neutralText = TokenUtil.nGramPartition("Maybe it's cloudy", 3);
        List<String> negativeText = TokenUtil.nGramPartition("I hate rain", 3);

        // Learn by classifying examples.
        // New categories can be added on the fly, when they are first used.
        // A classification consists of a category and a list of features
        // that resulted in the classification in that category.
        bayes.learn("positive", positiveText);
        bayes.learn("neutral", neutralText);
        bayes.learn("negative", negativeText);

        // Here are two unknown sentences to classify.
        List<String> unknownText1 = TokenUtil.nGramPartition("today is a sunny day", 3);
        List<String> unknownText2 = TokenUtil.nGramPartition("there will be rain", 3);
        List<String> unknownText3 = TokenUtil.nGramPartition("it will be cloudy", 3);

        System.out.println( // will output "positive"
                bayes.classify(unknownText1).getCategory());
        System.out.println( // will output "negative"
                bayes.classify(unknownText2).getCategory());
        System.out.println( // will output "neutral"
                bayes.classify(unknownText3).getCategory());

        // Get more detailed classification result.
        bayes.classifyDetailed(unknownText1);
    }

}
