# Elpris-analysator (CLI & Externt REST API)

**Repo för labb 1 i kursen Java 2026. Samtliga deluppgifter för godkänd nivå är implementerade:**

## Funktioner

1. Välja ett elområde
    - Data hämtas från API:et tillgängligt på: https://elprisetjustnu.se.
    - Priserna lagras i en omodifierbar lista så att informationen behåller sin integritet.
    - Flera fall av fel behandlas, ger feedback till användaren, och förhindrar att applikationen kraschar.
2. Visa lägsta, högsta pris och snittpris
    - Priserna avrundas till och visas i hela ören/kWh istället för en massa decimaler som är oviktiga för användaren.
    - Om inget elområde har valts blir användaren påmind att göra det först.
3. Sortera prislista lågt till högt
    - Tar den omodifierbara listan som input och klonar den för sorteringen så att den ursprungliga listan behåller sin
      integritet.
    - Formatterar varje rad med priset i öre/kWh och tiderna som priset gäller.
4. Visa bästa laddningstid
    - Använder algoritmen sliding window för beräkningen.
    - Då vi alltid analyserar för 4 sammanhängande timmar är fönsterstorleken statisk. Därför använder jag metoden med 2
      pekare som alltid pekar på indexet längst till vänster respektive höger i fönstret.
   - Metoden är byggd för prislistor uppdelade per 15 minuter. Därför är fönsterstorleken konstant 16.
    - Förutom att visa klockslagen för de 4 timmarna visar även utskriften vad medelpriset i ören/kWh kommer att vara
      mellan klockslagen.

## Utmaningar

Från början använde jag en vanlig array för prislistan. När jag hade gjort sorteringsmetoden och sedan färdigställde
metoden för bästa laddningstid stötte jag på problem. Eftersom arrayer är mutable och att jag skickat in arrayen till
sorteringsmetoden kastade den som väntat om ordningen på arrayen. Men då beräknas inte längre bästa laddningstid
korrekt, då den metoden kräver att arrayen är sorterad kronologiskt. Detta gav mig en insikt i hur arrayer fungerar i
relation till pass-by-value. När en array skickas som parameter kopieras inte arrayens innehåll, utan referensen till
arrayen, vilket var det jag hade missuppfattat. För att lösa detta gick jag över till att använda en omodifierbar List
för priserna så att prislistan förblir immutable.

# Referenser

**Källor jag använt mig av under implementeringen:**

- Format på namngivning av branches: https://conventionalbranch.org/
- Jackson ObjectMapper med
  arrayer: https://stackoverflow.com/questions/6349421/how-to-use-jackson-to-deserialise-an-array-of-objects
- Sortera arrayer med Comparator: https://www.baeldung.com/java-sorting-arrays
- Från String till datum: https://www.baeldung.com/java-string-to-date
- Sliding window algoritmen: https://leetcode.com/discuss/post/3722472/sliding-window-technique-a-comprehensive-ix2k/
- Moderna switch-satser: https://www.baeldung.com/java-switch
