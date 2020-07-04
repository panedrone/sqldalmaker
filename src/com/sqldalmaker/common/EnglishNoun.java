package com.sqldalmaker.common;

import java.util.*;

/**
 * An experimental class that provides static methods for testing
 * and computing various properties of strings that are presumed
 * to be English nouns.
 *
 * @author James A. Mason
 * @version 1.01 2002 Feb
 */
public class EnglishNoun {
    /**
     * Tests whether the given (presumed) English noun is plural.
     * A word like "sheep" that can be either singular or plural
     * yields true.
     */
    public static boolean isPlural(String word) {
        word = word.toLowerCase();
        if (irregularPlurals.containsKey(word)) return true;
        if (word.length() <= 1) return false;
        // If it is not an irregular plural, it must end in -s,
        // but it must not be an irregular singular (like "bus")
        // nor end in -ss (like "boss").
        if (word.charAt(word.length() - 1) != 's') return false;
        if (irregularSingulars.containsKey(word)) return false;
        return !(word.length() >= 2 && word.charAt(word.length() - 2) == 's');
    }

    /**
     * Tests whether the given (presumed) English noun is singular.
     * A word like "sheep" that can be either singular or plural
     * yields true.
     */
    public static boolean isSingular(String word) {
        word = word.toLowerCase();
        if (irregularSingulars.containsKey(word)) return true;
        // If it is not an irregular singular, it must not be an
        // irregular plural (like "children"), and it must not end
        // in -s unless it ends in -ss (like "boss")).
        if (irregularPlurals.containsKey(word)) return false;
        if (word.length() <= 0) return false;
        if (word.charAt(word.length() - 1) != 's') return true;
        return word.length() >= 2 && word.charAt(word.length() - 2) == 's';
    }

    /**
     * Returns the plural of a given (presumed) English word.
     * The given word may be singular or (already) plural.
     */
    public static String pluralOf(String word) // NO_UCD (unused code)
    {
        word = word.toLowerCase();
        if (isPlural(word)) return word;
        Object singularLookup = irregularSingulars.get(word);
        // String plural = null;
        if (singularLookup != null) {
            if (singularLookup instanceof ArrayList)
                return (String) (((ArrayList<?>) singularLookup).get(0));
            else return (String) singularLookup;
        }
        int length = word.length();
        if (length <= 1) return word + "'s";
        char lastLetter = word.charAt(length - 1);
        char secondLast = word.charAt(length - 2);
        if ("sxzo".indexOf(lastLetter) >= 0 ||
                (lastLetter == 'h' && (secondLast == 's' || secondLast == 'c')))
            return word + "es";
        if (lastLetter == 'y') {
            if ("aeiou".indexOf(secondLast) >= 0)
                return word + "s";
            else return word.substring(0, length - 1) + "ies";
        }
        return word + "s";
    } // end pluralOf

    /**
     * Returns the singular of a given (presumed) English word.
     * The given word may be plural or (already) singular.
     */
    public static String singularOf(String word) {
        word = word.toLowerCase();
        if (isSingular(word)) return word;
        Object pluralLookup = irregularPlurals.get(word);
        // String singular = null;
        if (pluralLookup != null) {
            if (pluralLookup instanceof ArrayList)
                return (String) (((ArrayList<?>) pluralLookup).get(0));
            else return (String) pluralLookup;
        }
        int length = word.length();
        if (length <= 1) return word;
        char lastLetter = word.charAt(length - 1);
        if (lastLetter != 's') return word;  // no final -s
        char secondLast = word.charAt(length - 2);
        if (secondLast == '\'') return word.substring(0, length - 2);
        // remove -'s
        if (word.equalsIgnoreCase("gas")) return word;
        if (secondLast != 'e' || length <= 3)
            return word.substring(0, length - 1); // remove final -s
        // Word ends in -es and has length >= 4:
        char thirdLast = word.charAt(length - 3);
        if (thirdLast == 'i')  // -ies => -y
            return word.substring(0, length - 3) + "y";
        if (thirdLast == 'x')  // -xes => -x
            return word.substring(0, length - 2);
        if (length <= 4)       // e.g. uses => use
            return word.substring(0, length - 1);
        char fourthLast = word.charAt(length - 4);
        if (thirdLast == 'h' && (fourthLast == 'c' || fourthLast == 's'))
            // -ches or -shes => -ch or -sh
            return word.substring(0, length - 2);
        if (thirdLast == 's' && fourthLast == 's') // -sses => -ss
            return word.substring(0, length - 2);
        return word.substring(0, length - 1);  // keep the final e.
    } // end singularOf

    private static HashMap<String, Object> irregularSingulars = new HashMap<String, Object>(100);
    private static HashMap<String, Object> irregularPlurals = new HashMap<String, Object>(100);

    static {
        irregularSingulars.put("ache", "aches");
        irregularSingulars.put("alumna", "alumnae");
        irregularSingulars.put("alumnus", "alumni");
        irregularSingulars.put("axis", "axes");
        irregularSingulars.put("bison", "bison");
        ArrayList<String> busses = new ArrayList<String>(2);
        busses.add("buses");
        busses.add("busses");
        irregularSingulars.put("bus", busses);
        irregularSingulars.put("calf", "calves");
        irregularSingulars.put("caribou", "caribou");
        irregularSingulars.put("child", "children");
        irregularSingulars.put("datum", "data");
        irregularSingulars.put("deer", "deer");
        ArrayList<String> dice = new ArrayList<String>(2);
        dice.add("dies");
        dice.add("dice");
        irregularSingulars.put("die", dice);
        irregularSingulars.put("elf", "elves");
        irregularSingulars.put("elk", "elk");
        irregularSingulars.put("fish", "fish");
        irregularSingulars.put("foot", "feet");
        irregularSingulars.put("gentleman", "gentlemen");
        irregularSingulars.put("gentlewoman", "gentlewomen");
        irregularSingulars.put("go", "goes");
        irregularSingulars.put("goose", "geese");
        irregularSingulars.put("grouse", "grouse");
        irregularSingulars.put("half", "halves");
        ArrayList<String> hoof = new ArrayList<String>(2);
        hoof.add("hooves");
        hoof.add("hoofs");
        irregularSingulars.put("hoof", hoof);
        irregularSingulars.put("knife", "knives");
        irregularSingulars.put("leaf", "leaves");
        irregularSingulars.put("life", "lives");
        irregularSingulars.put("louse", "lice");
        irregularSingulars.put("man", "men");
        irregularSingulars.put("money", "monies");
        irregularSingulars.put("moose", "moose");
        irregularSingulars.put("mouse", "mice");
        ArrayList<String> octopus = new ArrayList<String>(3);
        octopus.add("octopodes");
        octopus.add("octopi");
        octopus.add("octopuses");
        irregularSingulars.put("octopus", octopus);
        irregularSingulars.put("ox", "oxen");
        irregularSingulars.put("plus", "pluses");
        irregularSingulars.put("quail", "quail");
        irregularSingulars.put("reindeer", "reindeer");
        ArrayList<String> scarf = new ArrayList<String>(2);
        scarf.add("scarves");
        scarf.add("scarfs");
        irregularSingulars.put("scarf", scarf);
        irregularSingulars.put("self", "selves");
        irregularSingulars.put("sheaf", "sheaves");
        irregularSingulars.put("sheep", "sheep");
        irregularSingulars.put("shelf", "shelves");
        irregularSingulars.put("squid", "squid");
        irregularSingulars.put("thief", "thieves");
        irregularSingulars.put("tooth", "teeth");
        irregularSingulars.put("wharf", "wharves");
        irregularSingulars.put("wife", "wives");
        irregularSingulars.put("wolf", "wolves");
        irregularSingulars.put("woman", "women");

        irregularPlurals.put("aches", "ache");
        irregularPlurals.put("alumnae", "alumna");
        irregularPlurals.put("alumni", "alumnus");
        ArrayList<String> axes = new ArrayList<String>(2);
        axes.add("axe");
        axes.add("axis");
        irregularPlurals.put("axes", axes);
        irregularPlurals.put("bison", "bison");
        irregularPlurals.put("buses", "bus");
        irregularPlurals.put("busses", "bus");
        irregularPlurals.put("brethren", "brother");
        irregularPlurals.put("caches", "cache");
        irregularPlurals.put("calves", "calf");
        irregularPlurals.put("cargoes", "cargo");
        irregularPlurals.put("caribou", "caribou");
        irregularPlurals.put("children", "child");
        irregularPlurals.put("data", "datum");
        irregularPlurals.put("deer", "deer");
        irregularPlurals.put("dice", "die");
        irregularPlurals.put("dies", "die");
        irregularPlurals.put("dominoes", "domino");
        irregularPlurals.put("echoes", "echo");
        irregularPlurals.put("elves", "elf");
        irregularPlurals.put("elk", "elk");
        irregularPlurals.put("embargoes", "embargo");
        irregularPlurals.put("fish", "fish");
        irregularPlurals.put("feet", "foot");
        irregularPlurals.put("gentlemen", "gentleman");
        irregularPlurals.put("gentlewomen", "gentlewoman");
        irregularPlurals.put("geese", "goose");
        irregularPlurals.put("goes", "go");
        irregularPlurals.put("grottoes", "grotto");
        irregularPlurals.put("grouse", "grouse");
        irregularPlurals.put("halves", "half");
        irregularPlurals.put("hooves", "hoof");
        irregularPlurals.put("knives", "knife");
        irregularPlurals.put("leaves", "leaf");
        irregularPlurals.put("lives", "life");
        irregularPlurals.put("lice", "louse");
        irregularPlurals.put("men", "man");
        irregularPlurals.put("monies", "money");
        irregularPlurals.put("moose", "moose");
        irregularPlurals.put("mottoes", "motto");
        irregularPlurals.put("mice", "mouse");
        irregularPlurals.put("octopi", "octopus");
        irregularPlurals.put("octopodes", "octopus");
        irregularPlurals.put("octopuses", "octopus");
        irregularPlurals.put("oxen", "ox");
        irregularPlurals.put("pies", "pie");
        irregularPlurals.put("pluses", "plus");
        irregularPlurals.put("posses", "posse");
        irregularPlurals.put("potatoes", "potato");
        irregularPlurals.put("quail", "quail");
        irregularPlurals.put("reindeer", "reindeer");
        irregularPlurals.put("scarves", "scarf");
        irregularPlurals.put("sheaves", "sheaf");
        irregularPlurals.put("sheep", "sheep");
        irregularPlurals.put("shelves", "shelf");
        irregularPlurals.put("squid", "squid");
        irregularPlurals.put("teeth", "tooth");
        irregularPlurals.put("thieves", "thief");
        irregularPlurals.put("ties", "tie");
        irregularPlurals.put("tomatoes", "tomato");
        irregularPlurals.put("wharves", "wharf");
        irregularPlurals.put("wives", "wife");
        irregularPlurals.put("wolves", "wolf");
        irregularPlurals.put("women", "woman");
    } // end static initialization block

} // end class EnglishNoun