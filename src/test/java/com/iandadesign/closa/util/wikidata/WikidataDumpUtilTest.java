package com.iandadesign.closa.util.wikidata;

import com.iandadesign.closa.model.Token;
import com.iandadesign.closa.model.WikidataEntity;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static com.iandadesign.closa.util.wikidata.WikidataDumpUtil.*;

public class WikidataDumpUtilTest {


    @Test
    public void testExists() {
        WikidataEntity tree = new WikidataEntity("Q10884");
        Assert.assertTrue(exists(tree));

        Assert.assertTrue(exists("Auschwitz", "en"));
    }


    @Test
    public void testGetEntityById() {
        WikidataEntity tree = getEntityById("Q10884");

        Assert.assertEquals("tree", tree.getLabel());
    }


    @Test
    public void testGetEntitiesByLabel() {
        List<WikidataEntity> trees = getEntitiesByLabel("tree", "en");
        System.out.println(trees);
        Assert.assertFalse(trees.isEmpty());

        List<WikidataEntity> jane = getEntitiesByLabel("Jane", "en");
        System.out.println(jane);
        Assert.assertFalse(jane.isEmpty());

        List<WikidataEntity> auschwitz = getEntitiesByLabel("Auschwitz", "en");
        System.out.println(auschwitz);
        Assert.assertFalse(auschwitz.isEmpty());

        List<WikidataEntity> three = getEntitiesByLabel("three", "en");
        System.out.println(three);
        Assert.assertFalse(three.isEmpty());
    }

    @Test
    public void testGetEntitiesByToken() {
        Token auschwitz = new Token("Auschwitz", "Auschwitz", "NN", Token.NamedEntityType.LOCATION);

        List<WikidataEntity> auschwitzEntities = getEntitiesByToken(auschwitz, "en");

        System.out.println(auschwitzEntities);
        Assert.assertTrue(auschwitzEntities.size() > 0);


        Token jane = new Token("Jane", "Jane", "NN", Token.NamedEntityType.PERSON);

        List<WikidataEntity> janeEntities = getEntitiesByToken(jane, "en");

        System.out.println(janeEntities);
        Assert.assertTrue(janeEntities.size() > 0);

        Token femaleGivenName = new Token("female given name", "female given name", "NN", Token.NamedEntityType.O);

        List<WikidataEntity> femaleGivenNameEntities = getEntitiesByToken(femaleGivenName, "en");

        System.out.println(femaleGivenNameEntities);
        Assert.assertTrue(femaleGivenNameEntities.size() > 0);
    }

    @Test
    public void testGetEntitiesByTokenJapanese() {
        Token tokyo = new Token("東京", "東京", "名詞,固有名詞,地域,一般", Token.NamedEntityType.LOCATION);

        List<WikidataEntity> tokyoEntities = getEntitiesByToken(tokyo, "ja");

        System.out.println(tokyoEntities);
        Assert.assertTrue(tokyoEntities.size() > 0);
    }

    @Test
    public void testGetProperty() {
        String countryProperty = "P17";
        WikidataEntity chineseLanguage = new WikidataEntity("Q7850");
        WikidataEntity peoplesRepublicOfChina = new WikidataEntity("Q148");

        WikidataEntity tree = new WikidataEntity("Q10884");
        String subclassOfProperty = "P279";
        // WikidataEntity humanLanguage = new WikidataEntity("Q20162172");

        if (isHumanLanguage(chineseLanguage)) {
            List<WikidataEntity> propertyValues = getProperty(chineseLanguage, countryProperty);
            Assert.assertTrue(propertyValues.contains(peoplesRepublicOfChina));

            List<WikidataEntity> nonExistantPropertyValues = getProperty(chineseLanguage, "P1");
            Assert.assertTrue(nonExistantPropertyValues.isEmpty());

            List<WikidataEntity> multiplePropertyValues = getProperty(tree, subclassOfProperty);
            System.out.println(multiplePropertyValues);
        } else {
            Assert.fail();
        }
    }

    @Test
    public void testSubclassOf() {
        WikidataEntity tree = new WikidataEntity("Q10884", "tree");

        List<WikidataEntity> subclassOf = subclassOf(tree);

        WikidataEntity perennialPlant = new WikidataEntity("Q157957", "perennial plant");
        WikidataEntity woodyPlant = new WikidataEntity("Q757163", "woody plant");

        Assert.assertTrue(subclassOf.contains(perennialPlant));
        Assert.assertTrue(subclassOf.contains(woodyPlant));

        WikidataEntity highCourt = new WikidataEntity("Q16984027");
        System.out.println(subclassOf(highCourt));

        WikidataEntity skid = new WikidataEntity("Q4186259", "skid");
        System.out.println(subclassOf(skid));
    }

    @Test
    public void testGetSubclasses() {
        WikidataEntity perennialPlant = new WikidataEntity("Q157957", "perennial plant");

        System.out.println(getSubclasses(perennialPlant));
    }

    @Test
    public void testInstanceOf() {
        WikidataEntity mars = new WikidataEntity("Q111");
        WikidataEntity innerPlanet = new WikidataEntity("Q3504248");

        Assert.assertTrue(instanceOf(mars).contains(innerPlanet));

        WikidataEntity skid = new WikidataEntity("Q4186259", "skid");
        System.out.println(instanceOf(skid));
        // depending on the dump version, the above entity is instance or not
    }

    @Test
    public void testGetInstances() {
        WikidataEntity tree = new WikidataEntity("Q10884", "tree");

        System.out.println(getInstances(tree));
    }


    @Test
    public void testIsTransitiveInstanceOf() {
        WikidataEntity pi = new WikidataEntity("Q167", "pi");
        WikidataEntity number = new WikidataEntity("Q11563", "number");
        WikidataEntity creativeWork = new WikidataEntity("Q17537576", "creative work");

        Assert.assertTrue(isTransitiveInstanceOf(pi, number));
        Assert.assertFalse(isTransitiveInstanceOf(pi, creativeWork));
    }


    @Test
    public void testIsAncestor() {
        WikidataEntity tree = new WikidataEntity("Q10884");
        WikidataEntity perennialPlant = new WikidataEntity("Q157957");
        WikidataEntity organism = new WikidataEntity("Q7239");

        Assert.assertTrue(isAncestor(tree, perennialPlant));
        Assert.assertTrue(isAncestor(tree, organism));
    }


    @Test
    public void testGetDistanceToAncestor() {
        WikidataEntity tree = new WikidataEntity("Q10884");
        WikidataEntity perennialPlant = new WikidataEntity("Q157957");
        WikidataEntity organism = new WikidataEntity("Q7239");

        System.out.println(getDistanceToAncestor(tree, perennialPlant));
        System.out.println(getDistanceToAncestor(tree, organism));

        WikidataEntity volkswagen = new WikidataEntity("Q246");
        WikidataEntity automobileMarque = new WikidataEntity("Q17412622");

        System.out.println(getDistanceToAncestor(volkswagen, automobileMarque));

        Assert.assertTrue(getDistanceToAncestor(tree, perennialPlant) == 1);
        Assert.assertTrue(getDistanceToAncestor(tree, organism) == 3);
        Assert.assertTrue(getDistanceToAncestor(volkswagen, automobileMarque) == 1);
    }


    @Test
    public void testGetRootDistance() {
        WikidataEntity entity = new WikidataEntity("Q35120", "entity");
        long depthEntity = getRootDistance(entity);
        Assert.assertTrue(depthEntity == 0);

        WikidataEntity object = new WikidataEntity("Q488383", "object");
        long depthObject = getRootDistance(object);
        System.out.println(depthObject);
        Assert.assertTrue(depthObject == 1);

        WikidataEntity perceptibleObject = new WikidataEntity("Q337060", "perceptible object");
        long depthPerceptibleObject = getRootDistance(perceptibleObject);
        Assert.assertTrue(depthPerceptibleObject == 2);
    }

    @Test
    public void testGetRootDistanceOverInstances() {
        WikidataEntity mars = new WikidataEntity("Q111");

        Assert.assertTrue(getRootDistance(mars) == 9);
    }

    @Test
    public void testGetAllAncestors() {
        WikidataEntity tree = new WikidataEntity("Q10884");

        List<WikidataEntity> allAncestors = getAllAncestors(tree);

        Assert.assertTrue(allAncestors.contains(new WikidataEntity("Q35120", "entity")));
        Assert.assertTrue(allAncestors.contains(new WikidataEntity("Q756", "plant")));

    }


    @Test
    public void testGetAllDescendants() {
        WikidataEntity tree = new WikidataEntity("Q10884");

        List<WikidataEntity> allDescendants = getAllDescendants(tree);

        Assert.assertTrue(allDescendants.contains(new WikidataEntity("Q47128", "Christmas tree")));
    }


    @Test
    public void testGetMostSpecificParentEntity() {
        WikidataEntity mosque = new WikidataEntity("Q32815");
        WikidataEntity synagogue = new WikidataEntity("Q34627");

        WikidataEntity mostSpecificParentEntity = getMostSpecificParentEntity(mosque, synagogue);

        System.out.println(mostSpecificParentEntity);

        WikidataEntity placeOfWorship = new WikidataEntity("Q1370598");
        WikidataEntity temple = new WikidataEntity("Q44539");
        Assert.assertTrue(mostSpecificParentEntity.equals(placeOfWorship) || mostSpecificParentEntity.equals(temple));

        WikidataEntity tree = new WikidataEntity("Q10884");
        WikidataEntity perennialPlant = new WikidataEntity("Q157957");
        Assert.assertTrue(getMostSpecificParentEntity(tree, perennialPlant).equals(perennialPlant));
    }


    @Test
    public void testIsNaturalNumber() {
        WikidataEntity one = new WikidataEntity("Q199", "1");
        WikidataEntity pi = new WikidataEntity("Q167", "pi");

        Assert.assertTrue(isNaturalNumber(one));
        Assert.assertFalse(isNaturalNumber(pi));
    }

    @Test
    public void testIsCreativeWork() {
        WikidataEntity theMinisters = new WikidataEntity("Q7751572", "The Ministers");
        WikidataEntity isna = new WikidataEntity("Q1672387", "ISNA");
        WikidataEntity starWars = new WikidataEntity("Q462");

        Assert.assertTrue(isCreativeWork(theMinisters));
        Assert.assertFalse(isCreativeWork(isna));
        Assert.assertTrue(isCreativeWork(starWars));
    }

    @Test
    public void testIsGene() {
        WikidataEntity hi = new WikidataEntity("Q29724302", "hi");
        Assert.assertTrue(isGene(hi));
    }


    @Test
    public void testIsHuman() {
        WikidataEntity tomHanks = new WikidataEntity("Q2263", "Tom Hanks");
        Assert.assertTrue(isHuman(tomHanks));
    }

    @Test
    public void testIsLocation() {
        WikidataEntity auschwitz = new WikidataEntity("Q7342", "Oświęcim");
        Assert.assertTrue(isLocation(auschwitz));
    }

    @Test
    public void testIsOrganization() {
        WikidataEntity apple = new WikidataEntity("Q312", "Apple Inc.");
        Assert.assertTrue(isOrganization(apple));
    }

    @Test
    public void testIsHumanLanguage() {
        WikidataEntity chineseLanguage = new WikidataEntity("Q7850");
        Assert.assertTrue(isHumanLanguage(chineseLanguage));
    }

    @Test
    public void testCapitalizeIfFirstLetterIsUppercase() {
        Assert.assertTrue(capitalizeIfFirstLetterIsUppercase("Central African republic")
                .contains("Republic"));
    }


}
