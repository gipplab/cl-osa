package com.iandadesign.closa.util.wikidata;

import com.iandadesign.closa.model.Token;
import com.iandadesign.closa.model.WikidataEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


class WikidataDumpUtilTest {

    @Test
    void exists() {
        WikidataEntity tree = new WikidataEntity("Q10884");
        Assertions.assertTrue(WikidataDumpUtil.exists(tree));

        Assertions.assertTrue(WikidataDumpUtil.exists("Auschwitz", "en"));
    }


    @Test
    void getEntityById() {
        WikidataEntity tree = WikidataDumpUtil.getEntityById("Q10884");

        Assertions.assertEquals("tree", tree.getLabel());
    }


    @Test
    void getEntitiesByLabel() {
        List<WikidataEntity> trees = WikidataDumpUtil.getEntitiesByLabel("tree", "en");
        Assertions.assertFalse(trees.isEmpty());

        List<WikidataEntity> jane = WikidataDumpUtil.getEntitiesByLabel("Jane", "en");
        Assertions.assertFalse(jane.isEmpty());

        List<WikidataEntity> auschwitz = WikidataDumpUtil.getEntitiesByLabel("Auschwitz", "en");
        Assertions.assertFalse(auschwitz.isEmpty());

        List<WikidataEntity> three = WikidataDumpUtil.getEntitiesByLabel("three", "en");
        Assertions.assertFalse(three.isEmpty());
    }

    @Test
    void getEntitiesByToken() {
        Token auschwitz = new Token("Auschwitz", "Auschwitz", "NN", Token.NamedEntityType.LOCATION);
        List<WikidataEntity> auschwitzEntities = WikidataDumpUtil.getEntitiesByToken(auschwitz, "en");
        Assertions.assertTrue(auschwitzEntities.size() > 0);

        Token jane = new Token("Jane", "Jane", "NN", Token.NamedEntityType.PERSON);
        List<WikidataEntity> janeEntities = WikidataDumpUtil.getEntitiesByToken(jane, "en");
        Assertions.assertTrue(janeEntities.size() > 0);

        Token femaleGivenName = new Token("female given name", "female given name", "NN", Token.NamedEntityType.O);
        List<WikidataEntity> femaleGivenNameEntities = WikidataDumpUtil.getEntitiesByToken(femaleGivenName, "en");
        Assertions.assertTrue(femaleGivenNameEntities.size() > 0);
    }

    @Test
    void getEntitiesByTokenJapanese() {
        Token tokyo = new Token("東京", "東京", "名詞,固有名詞,地域,一般", Token.NamedEntityType.LOCATION);

        List<WikidataEntity> tokyoEntities = WikidataDumpUtil.getEntitiesByToken(tokyo, "ja");

        System.out.println(tokyoEntities);
        Assertions.assertTrue(tokyoEntities.size() > 0);
    }

    @Test
    void getProperty() {
        String countryProperty = "P17";
        WikidataEntity chineseLanguage = new WikidataEntity("Q7850");
        WikidataEntity peoplesRepublicOfChina = new WikidataEntity("Q148");

        WikidataEntity tree = new WikidataEntity("Q10884");
        String subclassOfProperty = "P279";

        if (WikidataDumpUtil.isHumanLanguage(chineseLanguage)) {
            List<WikidataEntity> propertyValues = WikidataDumpUtil.getProperty(chineseLanguage, countryProperty);
            Assertions.assertTrue(propertyValues.contains(peoplesRepublicOfChina));

            List<WikidataEntity> nonExistantPropertyValues = WikidataDumpUtil.getProperty(chineseLanguage, "P1");
            Assertions.assertTrue(nonExistantPropertyValues.isEmpty());

            List<WikidataEntity> multiplePropertyValues = WikidataDumpUtil.getProperty(tree, subclassOfProperty);
        } else {
            Assertions.fail();
        }
    }

    @Test
    void subclassOf() {
        WikidataEntity tree = new WikidataEntity("Q10884", "tree");

        List<WikidataEntity> subclassOf = WikidataDumpUtil.subclassOf(tree);

        WikidataEntity perennialPlant = new WikidataEntity("Q157957", "perennial plant");
        WikidataEntity woodyPlant = new WikidataEntity("Q757163", "woody plant");

        Assertions.assertTrue(subclassOf.contains(perennialPlant));
        Assertions.assertTrue(subclassOf.contains(woodyPlant));

        WikidataEntity highCourt = new WikidataEntity("Q16984027");
        System.out.println(WikidataDumpUtil.subclassOf(highCourt));

        WikidataEntity skid = new WikidataEntity("Q4186259", "skid");
        System.out.println(WikidataDumpUtil.subclassOf(skid));
    }

    @Test
    void getSubclasses() {
        WikidataEntity perennialPlant = new WikidataEntity("Q157957", "perennial plant");
        Assertions.assertFalse(WikidataDumpUtil.getSubclasses(perennialPlant).isEmpty());
    }

    @Test
    void instanceOf() {
        WikidataEntity mars = new WikidataEntity("Q111");
        WikidataEntity innerPlanet = new WikidataEntity("Q3504248");

        Assertions.assertTrue(WikidataDumpUtil.instanceOf(mars).contains(innerPlanet));
    }

    @Test
    void getInstances() {
        WikidataEntity tree = new WikidataEntity("Q10884", "tree");
        Assertions.assertFalse(WikidataDumpUtil.getInstances(tree).isEmpty());
    }


    @Test
    void isTransitiveInstanceOf() {
        WikidataEntity pi = new WikidataEntity("Q167", "pi");
        WikidataEntity number = new WikidataEntity("Q11563", "number");
        WikidataEntity creativeWork = new WikidataEntity("Q17537576", "creative work");

        Assertions.assertTrue(WikidataDumpUtil.isTransitiveInstanceOf(pi, number));
        Assertions.assertFalse(WikidataDumpUtil.isTransitiveInstanceOf(pi, creativeWork));
    }


    @Test
    void isAncestor() {
        WikidataEntity tree = new WikidataEntity("Q10884");
        WikidataEntity perennialPlant = new WikidataEntity("Q157957");
        WikidataEntity organism = new WikidataEntity("Q7239");

        Assertions.assertTrue(WikidataDumpUtil.isAncestor(tree, perennialPlant));
        Assertions.assertTrue(WikidataDumpUtil.isAncestor(tree, organism));
    }


    @Test
    void getDistanceToAncestor() {
        WikidataEntity tree = new WikidataEntity("Q10884");
        WikidataEntity perennialPlant = new WikidataEntity("Q157957");
        WikidataEntity organism = new WikidataEntity("Q7239");

        WikidataEntity volkswagen = new WikidataEntity("Q246");
        WikidataEntity automobileMarque = new WikidataEntity("Q17412622");

        Assertions.assertEquals(1, WikidataDumpUtil.getDistanceToAncestor(tree, perennialPlant));
        Assertions.assertEquals(3, WikidataDumpUtil.getDistanceToAncestor(tree, organism));
        Assertions.assertEquals(1, WikidataDumpUtil.getDistanceToAncestor(volkswagen, automobileMarque));
    }


    @Test
    void getRootDistance() {
        WikidataEntity entity = new WikidataEntity("Q35120", "entity");
        long depthEntity = WikidataDumpUtil.getRootDistance(entity);
        Assertions.assertEquals(0, depthEntity);

        WikidataEntity object = new WikidataEntity("Q488383", "object");
        long depthObject = WikidataDumpUtil.getRootDistance(object);
        Assertions.assertEquals(1, depthObject);

        WikidataEntity perceptibleObject = new WikidataEntity("Q337060", "perceptible object");
        long depthPerceptibleObject = WikidataDumpUtil.getRootDistance(perceptibleObject);
        Assertions.assertEquals(2, depthPerceptibleObject);
    }

    @Test
    void getRootDistanceOverInstances() {
        WikidataEntity mars = new WikidataEntity("Q111");

        Assertions.assertEquals(9, WikidataDumpUtil.getRootDistance(mars));
    }

    @Test
    void getAllAncestors() {
        WikidataEntity tree = new WikidataEntity("Q10884");

        List<WikidataEntity> allAncestors = WikidataDumpUtil.getAllAncestors(tree);

        Assertions.assertTrue(allAncestors.contains(new WikidataEntity("Q35120", "entity")));
        Assertions.assertTrue(allAncestors.contains(new WikidataEntity("Q756", "plant")));

    }


    @Test
    void getAllDescendants() {
        WikidataEntity tree = new WikidataEntity("Q10884");

        List<WikidataEntity> allDescendants = WikidataDumpUtil.getAllDescendants(tree);

        Assertions.assertTrue(allDescendants.contains(new WikidataEntity("Q47128", "Christmas tree")));
    }


    @Test
    void getMostSpecificParentEntity() {
        WikidataEntity mosque = new WikidataEntity("Q32815");
        WikidataEntity synagogue = new WikidataEntity("Q34627");

        WikidataEntity mostSpecificParentEntity = WikidataDumpUtil.getMostSpecificParentEntity(mosque, synagogue);

        WikidataEntity placeOfWorship = new WikidataEntity("Q1370598");
        WikidataEntity temple = new WikidataEntity("Q44539");
        Assertions.assertTrue(mostSpecificParentEntity.equals(placeOfWorship) || mostSpecificParentEntity.equals(temple));

        WikidataEntity tree = new WikidataEntity("Q10884");
        WikidataEntity perennialPlant = new WikidataEntity("Q157957");
        Assertions.assertEquals(WikidataDumpUtil.getMostSpecificParentEntity(tree, perennialPlant), perennialPlant);
    }


    @Test
    void isNaturalNumber() {
        WikidataEntity one = new WikidataEntity("Q199", "1");
        WikidataEntity pi = new WikidataEntity("Q167", "pi");

        Assertions.assertTrue(WikidataDumpUtil.isNaturalNumber(one));
        Assertions.assertFalse(WikidataDumpUtil.isNaturalNumber(pi));
    }

    @Test
    void isCreativeWork() {
        WikidataEntity theMinisters = new WikidataEntity("Q7751572", "The Ministers");
        WikidataEntity isna = new WikidataEntity("Q1672387", "ISNA");
        WikidataEntity starWars = new WikidataEntity("Q462");

        Assertions.assertTrue(WikidataDumpUtil.isCreativeWork(theMinisters));
        Assertions.assertFalse(WikidataDumpUtil.isCreativeWork(isna));
        Assertions.assertTrue(WikidataDumpUtil.isCreativeWork(starWars));
    }

    @Test
    void testIsGene() {
        WikidataEntity hi = new WikidataEntity("Q29724302", "hi");
        Assertions.assertTrue(WikidataDumpUtil.isGene(hi));
    }


    @Test
    void isHuman() {
        WikidataEntity tomHanks = new WikidataEntity("Q2263", "Tom Hanks");
        Assertions.assertTrue(WikidataDumpUtil.isHuman(tomHanks));
    }

    @Test
    void isLocation() {
        WikidataEntity auschwitz = new WikidataEntity("Q7342", "Oświęcim");
        Assertions.assertTrue(WikidataDumpUtil.isLocation(auschwitz));
    }

    @Test
    void isOrganization() {
        WikidataEntity apple = new WikidataEntity("Q312", "Apple Inc.");
        Assertions.assertTrue(WikidataDumpUtil.isOrganization(apple));
    }

    @Test
    void isHumanLanguage() {
        WikidataEntity chineseLanguage = new WikidataEntity("Q7850");
        Assertions.assertTrue(WikidataDumpUtil.isHumanLanguage(chineseLanguage));
    }

    @Test
    void capitalizeIfFirstLetterIsUppercase() {
        Assertions.assertTrue(WikidataDumpUtil.capitalizeIfFirstLetterIsUppercase("Central African republic")
                .contains("Republic"));
    }

    @Test
    void getProperties() {
        Assertions.assertTrue(WikidataDumpUtil.getProperties(new WikidataEntity("Q1")).containsKey("P279"));
    }

    @Test
    void getSiteLinkInLanguage() {
        String englishSiteLink = "New York City";

        Assertions.assertEquals(WikidataDumpUtil.getSiteLinkInLanguage(englishSiteLink, "af"), "New York Stad");
    }

    @Test
    void getSiteLinkInEnglish() {
        String afrikaansSiteLink = "New York Stad";

        Assertions.assertEquals(WikidataDumpUtil.getSiteLinkInEnglish(afrikaansSiteLink, "af"), "New York City");
    }
}
