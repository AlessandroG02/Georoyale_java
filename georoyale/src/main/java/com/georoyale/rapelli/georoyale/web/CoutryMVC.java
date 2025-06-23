package com.georoyale.rapelli.georoyale.web;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.georoyale.rapelli.georoyale.entities.Country;
import com.georoyale.rapelli.georoyale.entities.User;
import com.georoyale.rapelli.georoyale.entities.Points;
import com.georoyale.rapelli.georoyale.services.CountryService;
import com.georoyale.rapelli.georoyale.services.UserService;
import com.georoyale.rapelli.georoyale.repos.PointsRepo;
import com.georoyale.rapelli.georoyale.repos.UserRepo;

import jakarta.servlet.http.HttpSession;

@Controller
public class CoutryMVC {

    @Autowired
    private CountryService service;

    @Autowired
    private UserService userService;

    @Autowired
    private PointsRepo pointsRepo;

    @Autowired
    private UserRepo userRepo;

    @GetMapping("")
    public String getHome() {
        return "home";
    }

    // ===== AUTHENTICATION METHODS =====

    @GetMapping("/login")
    public String getLogin() {
        return "login";
    }

    @GetMapping("/register")
    public String getRegister() {
        return "register";
    }

    @PostMapping("/register")
    public String processRegister(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            Model m) {

        // Validazione password
        if (!password.equals(confirmPassword)) {
            m.addAttribute("error", "Le password non corrispondono!");
            return "register";
        }

        // Controllo lunghezza password
        if (password.length() < 6) {
            m.addAttribute("error", "La password deve essere di almeno 6 caratteri!");
            return "register";
        }

        // Controllo username esistente
        if (userService.usernameExists(username)) {
            m.addAttribute("error", "Username già esistente! Scegline un altro.");
            return "register";
        }

        try {
            // Crea nuovo utente
            userService.createUser(username, password);
            m.addAttribute("success", "Registrazione completata! Ora puoi accedere.");
            return "register";
        } catch (Exception e) {
            m.addAttribute("error", "Errore durante la registrazione. Riprova.");
            return "register";
        }
    }

    @GetMapping("/profile")
    public String getProfile(Model m) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            String username = auth.getName();
            User user = userService.findByUsername(username);

            if (user != null) {
                // Calcola statistiche dai quiz
                List<Points> userPoints = pointsRepo.findByUserOrderByDataPointDesc(user);
                int totalQuizzes = userPoints.size();
                int totalPoints = userPoints.stream().mapToInt(Points::getTotPoint).sum();
                int bestScore = userPoints.stream().mapToInt(Points::getTotPoint).max().orElse(0);

                // Aggiorna i campi transient dell'utente
                user.setTotalQuizzes(totalQuizzes);
                user.setCorrectAnswers(totalPoints / 10); // Approssimazione basata su 10 punti per risposta
                user.setBestStreak(bestScore);

                m.addAttribute("user", user);
                m.addAttribute("totalPoints", totalPoints);
                m.addAttribute("bestScore", bestScore);
            }
        }

        return "profile";
    }

    @GetMapping("/logout")
    public String logout() {
        return "redirect:/";
    }

    // ===== METODO HELPER PER SALVARE PUNTEGGI =====

    private void saveQuizScore(String username, int score, String quizType) {
        try {
            User user = userService.findByUsername(username);
            if (user != null && score > 0) { // Salva solo se ha fatto punti
                Points points = new Points();
                points.setUser(user);
                points.setTotPoint(score);
                points.setDataPoint(LocalDateTime.now());

                Points savedPoints = pointsRepo.save(points);

                // Aggiorna l'id_point dell'utente con l'ultimo punteggio
                user.setIdPoint(savedPoints.getIdPoint().intValue());
                userRepo.save(user);

            }
        } catch (Exception e) {
            System.err.println("Errore nel salvataggio punteggio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===== METODO PER VEDERE LA CRONOLOGIA QUIZ =====

    @GetMapping("/quiz/history")
    public String showQuizHistory(Model m) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            String username = auth.getName();
            User user = userService.findByUsername(username);

            if (user != null) {
                List<Points> history = pointsRepo.findByUserOrderByDataPointDesc(user);
                Integer totalPoints = history.stream()
                        .mapToInt(Points::getTotPoint)
                        .sum();
                Integer maxScore = history.stream()
                        .mapToInt(Points::getTotPoint)
                        .max()
                        .orElse(0);

                m.addAttribute("history", history);
                m.addAttribute("totalPoints", totalPoints);
                m.addAttribute("maxScore", maxScore);
                m.addAttribute("totalQuizzes", history.size());
            }
        }

        return "quiz_history";
    }

    // ===== METODO HELPER PER TRACKING SESSIONE =====

    private void trackAnswer(HttpSession session, boolean isCorrect) {
        // Contatori semplici
        Integer totalAnswers = (Integer) session.getAttribute("totalAnswers");
        Integer correctAnswers = (Integer) session.getAttribute("correctAnswers");
        Integer currentStreak = (Integer) session.getAttribute("currentStreak");
        Integer bestStreak = (Integer) session.getAttribute("bestStreak");

        // Inizializza se prima volta
        if (totalAnswers == null) {
            totalAnswers = 0;
            correctAnswers = 0;
            currentStreak = 0;
            bestStreak = 0;
        }

        totalAnswers++;

        if (isCorrect) {
            correctAnswers++;
            currentStreak++;
            if (currentStreak > bestStreak) {
                bestStreak = currentStreak;
            }
        } else {
            currentStreak = 0;
        }

        // Salva in sessione
        session.setAttribute("totalAnswers", totalAnswers);
        session.setAttribute("correctAnswers", correctAnswers);
        session.setAttribute("currentStreak", currentStreak);
        session.setAttribute("bestStreak", bestStreak);
    }

    // Metodo per passare stats al template
    private void addSessionStats(HttpSession session, Model m) {
        Integer totalAnswers = (Integer) session.getAttribute("totalAnswers");
        Integer correctAnswers = (Integer) session.getAttribute("correctAnswers");
        Integer currentStreak = (Integer) session.getAttribute("currentStreak");
        Integer bestStreak = (Integer) session.getAttribute("bestStreak");

        // Valori di default se null
        totalAnswers = totalAnswers != null ? totalAnswers : 0;
        correctAnswers = correctAnswers != null ? correctAnswers : 0;
        currentStreak = currentStreak != null ? currentStreak : 0;
        bestStreak = bestStreak != null ? bestStreak : 0;

        double accuracy = totalAnswers > 0 ? (correctAnswers * 100.0) / totalAnswers : 0.0;

        m.addAttribute("sessionTotal", totalAnswers);
        m.addAttribute("sessionCorrect", correctAnswers);
        m.addAttribute("sessionStreak", currentStreak);
        m.addAttribute("sessionBestStreak", bestStreak);
        m.addAttribute("sessionAccuracy", String.format("%.1f", accuracy));
    }

    // ===== METODI ALLENAMENTO (SENZA PUNTEGGI) =====

    @GetMapping("allenamento")
    public String getAllenamento(HttpSession session, Model m) {
        addSessionStats(session, m);
        return "allenamento";
    }

    @GetMapping("allenamento_bandiera")
    public String getAllenamentoBandiera(HttpSession session, Model m) {
        Country c1 = service.getRandomCountry();
        Country c2 = service.getRandomCountry();

        List<Country> bandiere = new ArrayList<>();
        bandiere.add(c1);
        bandiere.add(c2);
        Collections.shuffle(bandiere);

        m.addAttribute("countryName", c1.getName());
        m.addAttribute("countryFlag", c1.getFlag());
        m.addAttribute("countryCode", c1.getAlphaCode());
        m.addAttribute("bandiere", bandiere);

        addSessionStats(session, m);
        return "allenamento_bandiera";
    }

    @PostMapping("allenamento_bandiera_answer")
    public String processAllenamentoBandieraAnswer(
            @RequestParam String countryCode,
            @RequestParam String correctAnswer,
            @RequestParam String selectedAnswer,
            HttpSession session,
            Model m) {

        List<Country> countries = service.getCountries();
        Country correctCountry = countries.stream()
                .filter(c -> c.getAlphaCode().equals(countryCode))
                .findFirst()
                .orElse(null);

        boolean isCorrect = correctAnswer.equals(selectedAnswer);

        // TRACCIA LA RISPOSTA (solo sessione, no database)
        trackAnswer(session, isCorrect);

        m.addAttribute("countryName", correctCountry.getName());
        m.addAttribute("countryFlag", correctCountry.getFlag());
        m.addAttribute("correctAnswer", correctAnswer);
        m.addAttribute("selectedAnswer", selectedAnswer);
        m.addAttribute("isCorrect", isCorrect);

        addSessionStats(session, m);
        return "allenamento_bandiera_result";
    }

    @GetMapping("allenamento_capitale")
    public String getAllenamentoCapitale(HttpSession session, Model m) {
        Country c1 = service.getRandomCountry();
        Country c2 = service.getRandomCountry();

        List<Country> capitali = new ArrayList<>();
        capitali.add(c1);
        capitali.add(c2);
        Collections.shuffle(capitali);

        m.addAttribute("countryName", c1.getName());
        m.addAttribute("countryCapital", c1.getCapital());
        m.addAttribute("countryCode", c1.getAlphaCode());
        m.addAttribute("capitali", capitali);

        addSessionStats(session, m);
        return "allenamento_capitale";
    }
    
    @PostMapping("allenamento_capitale_answer")
    public String processAllenamentoCapitaleAnswer(
            @RequestParam String correctAnswer,
            @RequestParam String selectedAnswer,
            HttpSession session,
            Model m) {

        boolean isCorrect = correctAnswer.equals(selectedAnswer);

        // TRACCIA LA RISPOSTA (solo sessione, no database)
        trackAnswer(session, isCorrect);

        m.addAttribute("correctAnswer", correctAnswer);
        m.addAttribute("selectedAnswer", selectedAnswer);
        m.addAttribute("isCorrect", isCorrect);

        addSessionStats(session, m);
        return "allenamento_capitale_result";
    }

    // ===== QUIZ SESSIONS (10 DOMANDE) =====

    @GetMapping("quiz_capital_session")
    public String startQuizCapitalSession(HttpSession session, Model m) {
        session.setAttribute("quizType", "capital");
        session.setAttribute("currentQuestionNumber", 1);
        session.setAttribute("sessionScore", 0);
        session.setAttribute("sessionAnswers", new ArrayList<Boolean>());
        session.setAttribute("userAnswers", new ArrayList<String>());
        session.setAttribute("correctAnswersList", new ArrayList<String>());
        session.setAttribute("questionCountries", new ArrayList<Country>());

        m.addAttribute("isSession", true);
        m.addAttribute("currentQuestionNumber", 1);
        m.addAttribute("totalQuestions", 10);
        m.addAttribute("sessionScore", 0);

        return getQuizCapital(session, m);
    }

    @PostMapping("quiz_capital_session_answer")
    public String processQuizCapitalSessionAnswer(
            @RequestParam String countryCode,
            @RequestParam String correctAnswer,
            @RequestParam String selectedAnswer,
            HttpSession session,
            Model m) {

        String quizType = (String) session.getAttribute("quizType");
        if (!"capital".equals(quizType)) {
            return processQuizCapitalAnswer(countryCode, correctAnswer, selectedAnswer, session, m);
        }

        return processSessionAnswer("capital", correctAnswer, selectedAnswer, session, m, () -> {
            Country randomCountry = service.getRandomCountry();
            List<Country> allCountries = service.getCountries();
            Collections.shuffle(allCountries);

            List<Country> options = new ArrayList<>();
            options.add(randomCountry);

            for (Country country : allCountries) {
                if (!country.getAlphaCode().equals(randomCountry.getAlphaCode()) && options.size() < 4) {
                    options.add(country);
                }
            }
            Collections.shuffle(options);

            m.addAttribute("countryName", randomCountry.getName());
            m.addAttribute("countryFlag", randomCountry.getFlag());
            m.addAttribute("countryCode", randomCountry.getAlphaCode());
            m.addAttribute("correctCapital", randomCountry.getCapital());
            m.addAttribute("options", options);

            session.setAttribute("currentCountryCode", randomCountry.getAlphaCode());

            return "quiz_capital";
        });
    }

    @GetMapping("quiz_nation_session")
    public String startQuizNationSession(HttpSession session, Model m) {
        session.setAttribute("quizType", "nation");
        session.setAttribute("currentQuestionNumber", 1);
        session.setAttribute("sessionScore", 0);
        session.setAttribute("sessionAnswers", new ArrayList<Boolean>());
        session.setAttribute("userAnswers", new ArrayList<String>());
        session.setAttribute("correctAnswersList", new ArrayList<String>());
        session.setAttribute("questionCountries", new ArrayList<Country>());

        m.addAttribute("isSession", true);
        m.addAttribute("currentQuestionNumber", 1);
        m.addAttribute("totalQuestions", 10);
        m.addAttribute("sessionScore", 0);

        return getQuizNation(session, m);
    }

    @PostMapping("quiz_nation_session_answer")
    public String processQuizNationSessionAnswer(
            @RequestParam String countryCode,
            @RequestParam String correctAnswer,
            @RequestParam String selectedAnswer,
            HttpSession session,
            Model m) {

        String quizType = (String) session.getAttribute("quizType");
        if (!"nation".equals(quizType)) {
            return processQuizNationAnswer(countryCode, correctAnswer, selectedAnswer, session, m);
        }

        session.setAttribute("currentCountryCode", countryCode);

        return processSessionAnswer("nation", correctAnswer, selectedAnswer, session, m, () -> {
            Country randomCountry = service.getRandomCountry();
            List<Country> allCountries = service.getCountries();
            Collections.shuffle(allCountries);

            List<Country> options = new ArrayList<>();
            options.add(randomCountry);

            for (Country country : allCountries) {
                if (!country.getAlphaCode().equals(randomCountry.getAlphaCode()) && options.size() < 4) {
                    options.add(country);
                }
            }
            Collections.shuffle(options);

            m.addAttribute("capitalName", randomCountry.getCapital());
            m.addAttribute("countryCode", randomCountry.getAlphaCode());
            m.addAttribute("correctCountry", randomCountry.getName());
            m.addAttribute("options", options);

            return "quiz_nation";
        });
    }

    @GetMapping("quiz_flag_session")
    public String startQuizFlagSession(HttpSession session, Model m) {
        session.setAttribute("quizType", "flag");
        session.setAttribute("currentQuestionNumber", 1);
        session.setAttribute("sessionScore", 0);
        session.setAttribute("sessionAnswers", new ArrayList<Boolean>());
        session.setAttribute("userAnswers", new ArrayList<String>());
        session.setAttribute("correctAnswersList", new ArrayList<String>());
        session.setAttribute("questionCountries", new ArrayList<Country>());

        m.addAttribute("isSession", true);
        m.addAttribute("currentQuestionNumber", 1);
        m.addAttribute("totalQuestions", 10);
        m.addAttribute("sessionScore", 0);

        return getQuizFlag(session, m);
    }

    @PostMapping("quiz_flag_session_answer")
    public String processQuizFlagSessionAnswer(
            @RequestParam String countryCode,
            @RequestParam String correctAnswer,
            @RequestParam String selectedAnswer,
            HttpSession session,
            Model m) {

        String quizType = (String) session.getAttribute("quizType");
        if (!"flag".equals(quizType)) {
            return processQuizFlagAnswer(countryCode, correctAnswer, selectedAnswer, session, m);
        }

        session.setAttribute("currentCountryCode", countryCode);

        return processSessionAnswer("flag", correctAnswer, selectedAnswer, session, m, () -> {
            Country randomCountry = service.getRandomCountry();
            List<Country> allCountries = service.getCountries();
            Collections.shuffle(allCountries);

            List<Country> options = new ArrayList<>();
            options.add(randomCountry);

            for (Country country : allCountries) {
                if (!country.getAlphaCode().equals(randomCountry.getAlphaCode()) && options.size() < 4) {
                    options.add(country);
                }
            }
            Collections.shuffle(options);

            m.addAttribute("countryFlag", randomCountry.getFlag());
            m.addAttribute("countryCode", randomCountry.getAlphaCode());
            m.addAttribute("correctCountry", randomCountry.getName());
            m.addAttribute("options", options);

            return "quiz_flag";
        });
    }

    @GetMapping("quiz_continent_session")
    public String startQuizContinentSession(HttpSession session, Model m) {
        session.setAttribute("quizType", "continent");
        session.setAttribute("currentQuestionNumber", 1);
        session.setAttribute("sessionScore", 0);
        session.setAttribute("sessionAnswers", new ArrayList<Boolean>());
        session.setAttribute("userAnswers", new ArrayList<String>());
        session.setAttribute("correctAnswersList", new ArrayList<String>());
        session.setAttribute("questionCountries", new ArrayList<Country>());

        m.addAttribute("isSession", true);
        m.addAttribute("currentQuestionNumber", 1);
        m.addAttribute("totalQuestions", 10);
        m.addAttribute("sessionScore", 0);

        return getQuizContinent(session, m);
    }

    @PostMapping("quiz_continent_session_answer")
    public String processQuizContinentSessionAnswer(
            @RequestParam String countryCode,
            @RequestParam String correctAnswer,
            @RequestParam String selectedAnswer,
            HttpSession session,
            Model m) {

        String quizType = (String) session.getAttribute("quizType");
        if (!"continent".equals(quizType)) {
            return processQuizContinentAnswer(countryCode, correctAnswer, selectedAnswer, session, m);
        }

        session.setAttribute("currentCountryCode", countryCode);

        return processSessionAnswer("continent", correctAnswer, selectedAnswer, session, m, () -> {
            Country randomCountry = service.getRandomCountry();
            String regionFromDB = randomCountry.getRegion();

            m.addAttribute("countryName", randomCountry.getName());
            m.addAttribute("countryFlag", randomCountry.getFlag());
            m.addAttribute("capitalName", randomCountry.getCapital());
            m.addAttribute("countryCode", randomCountry.getAlphaCode());
            m.addAttribute("correctContinent", regionFromDB);

            return "quiz_continent";
        });
    }

    // ===== METODO HELPER PER PROCESSARE SESSIONI =====

    private String processSessionAnswer(String quizType, String correctAnswer, String selectedAnswer,
            HttpSession session, Model m, java.util.function.Supplier<String> nextQuestionGenerator) {

        Integer currentQ = (Integer) session.getAttribute("currentQuestionNumber");
        Integer sessionScore = (Integer) session.getAttribute("sessionScore");
        @SuppressWarnings("unchecked")
        List<Boolean> answers = (List<Boolean>) session.getAttribute("sessionAnswers");
        @SuppressWarnings("unchecked")
        List<String> userAnswers = (List<String>) session.getAttribute("userAnswers");
        @SuppressWarnings("unchecked")
        List<String> correctAnswersList = (List<String>) session.getAttribute("correctAnswersList");
        @SuppressWarnings("unchecked")
        List<Country> questionCountries = (List<Country>) session.getAttribute("questionCountries");

        if (currentQ == null)
            currentQ = 1;
        if (sessionScore == null)
            sessionScore = 0;
        if (answers == null)
            answers = new ArrayList<>();
        if (userAnswers == null)
            userAnswers = new ArrayList<>();
        if (correctAnswersList == null)
            correctAnswersList = new ArrayList<>();
        if (questionCountries == null)
            questionCountries = new ArrayList<>();

        // Controlla risposta
        boolean isCorrect = selectedAnswer.equals(correctAnswer);
        answers.add(isCorrect);
        userAnswers.add(selectedAnswer);
        correctAnswersList.add(correctAnswer);

        // Aggiungi il paese della domanda corrente
        String currentCountryCode = (String) session.getAttribute("currentCountryCode");
        if (currentCountryCode != null) {
            List<Country> countries = service.getCountries();
            Country currentCountry = countries.stream()
                    .filter(c -> c.getAlphaCode().equals(currentCountryCode))
                    .findFirst()
                    .orElse(null);
            if (currentCountry != null) {
                questionCountries.add(currentCountry);
            }
        }

        if (isCorrect) {
            sessionScore += ("higher_lower".equals(quizType)) ? 15 : 10;
        }

        // Aggiorna sessione
        session.setAttribute("sessionScore", sessionScore);
        session.setAttribute("sessionAnswers", answers);
        session.setAttribute("userAnswers", userAnswers);
        session.setAttribute("correctAnswersList", correctAnswersList);
        session.setAttribute("questionCountries", questionCountries);

        // Se abbiamo completato 10 domande
        if (currentQ >= 10) {
            // Salva punteggio finale
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                saveQuizScore(auth.getName(), sessionScore, "Quiz " +
                        quizType.substring(0, 1).toUpperCase() + quizType.substring(1) + " Session");
            }

            // Crea lista dettagliata dei risultati
            List<QuestionResult> detailedResults = new ArrayList<>();
            for (int i = 0; i < Math.min(answers.size(), questionCountries.size()); i++) {
                QuestionResult result = new QuestionResult();
                result.setQuestionNumber(i + 1);
                result.setCountry(questionCountries.get(i));
                result.setUserAnswer(i < userAnswers.size() ? userAnswers.get(i) : "");
                result.setCorrectAnswer(i < correctAnswersList.size() ? correctAnswersList.get(i) : "");
                result.setCorrect(i < answers.size() ? answers.get(i) : false);
                result.setQuizType(quizType);
                detailedResults.add(result);
            }

            // Mostra risultato finale con dettagli
            m.addAttribute("finalScore", sessionScore);
            m.addAttribute("correctAnswers", sessionScore / ("higher_lower".equals(quizType) ? 15 : 10));
            m.addAttribute("totalQuestions", 10);
            m.addAttribute("percentage", (sessionScore / ("higher_lower".equals(quizType) ? 150.0 : 100.0)) * 100.0);
            m.addAttribute("quizTypeName", quizType.substring(0, 1).toUpperCase() + quizType.substring(1));
            m.addAttribute("detailedResults", detailedResults);

            // Pulisci sessione
            session.removeAttribute("quizType");
            session.removeAttribute("currentQuestionNumber");
            session.removeAttribute("sessionScore");
            session.removeAttribute("sessionAnswers");
            session.removeAttribute("userAnswers");
            session.removeAttribute("correctAnswersList");
            session.removeAttribute("questionCountries");
            session.removeAttribute("currentCountryCode");

            return "quiz_session_detailed_result";
        } else {
            // Prossima domanda
            currentQ++;
            session.setAttribute("currentQuestionNumber", currentQ);

            // Genera prossima domanda usando il supplier
            String templateName = nextQuestionGenerator.get();

            // Aggiungi info sessione
            m.addAttribute("currentQuestionNumber", currentQ);
            m.addAttribute("totalQuestions", 10);
            m.addAttribute("sessionScore", sessionScore);
            m.addAttribute("isSession", true);

            return templateName;
        }
    }

    // ===== METODI QUIZ SINGOLI (CON PUNTEGGI) =====

    @GetMapping("quiz")
    public String getQuiz(HttpSession session, Model m) {
        addSessionStats(session, m);
        return "quiz";
    }

    @GetMapping("higher_lower")
    public String getHigherLower(HttpSession session, Model m) {
        Country country1 = service.getRandomCountry();
        Country country2 = service.getRandomCountry();

        while (country2.getAlphaCode().equals(country1.getAlphaCode())) {
            country2 = service.getRandomCountry();
        }

        m.addAttribute("country1", country1);
        m.addAttribute("country2", country2);

        addSessionStats(session, m);
        return "higher_lower";
    }

    @PostMapping("higher_lower_answer")
    public String processHigherLowerAnswer(
            @RequestParam String country1Code,
            @RequestParam String country2Code,
            @RequestParam String population1,
            @RequestParam String population2,
            @RequestParam String selectedCountry,
            HttpSession session,
            Model m) {

        List<Country> countries = service.getCountries();
        Country country1 = countries.stream()
                .filter(c -> c.getAlphaCode().equals(country1Code))
                .findFirst()
                .orElse(null);
        Country country2 = countries.stream()
                .filter(c -> c.getAlphaCode().equals(country2Code))
                .findFirst()
                .orElse(null);

        long pop1 = Long.parseLong(population1);
        long pop2 = Long.parseLong(population2);

        // Determina quale paese ha più abitanti
        boolean country1HasMore = pop1 > pop2;
        String correctCountryCode = country1HasMore ? country1Code : country2Code;

        // Controlla se l'utente ha scelto correttamente
        boolean isCorrect = selectedCountry.equals(correctCountryCode);

        // TRACCIA LA RISPOSTA
        trackAnswer(session, isCorrect);

        // SALVA PUNTEGGIO SE UTENTE LOGGATO
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            saveQuizScore(auth.getName(), isCorrect ? 15 : 0, "Higher or Lower");
        }

        // Determina il vincitore per il risultato
        String winnerName;
        long winnerPopulation;
        long populationDifference;

        if (pop1 > pop2) {
            winnerName = country1.getName();
            winnerPopulation = pop1;
            populationDifference = pop1 - pop2;
        } else {
            winnerName = country2.getName();
            winnerPopulation = pop2;
            populationDifference = pop2 - pop1;
        }

        // Determina quale paese ha scelto l'utente
        Country selectedCountryObj = selectedCountry.equals(country1Code) ? country1 : country2;

        m.addAttribute("country1Name", country1.getName());
        m.addAttribute("country1Flag", country1.getFlag());
        m.addAttribute("country2Name", country2.getName());
        m.addAttribute("country2Flag", country2.getFlag());
        m.addAttribute("selectedCountryName", selectedCountryObj.getName());
        m.addAttribute("selectedCountryFlag", selectedCountryObj.getFlag());
        m.addAttribute("isCorrect", isCorrect);
        m.addAttribute("population1", pop1);
        m.addAttribute("population2", pop2);
        m.addAttribute("winnerName", winnerName);
        m.addAttribute("winnerPopulation", winnerPopulation);
        m.addAttribute("populationDifference", populationDifference);

        addSessionStats(session, m);
        return "higher_lower_result";
    }

    @GetMapping("quiz_capital")
    public String getQuizCapital(HttpSession session, Model m) {
        Country randomCountry = service.getRandomCountry();
        List<Country> allCountries = service.getCountries();
        Collections.shuffle(allCountries);

        List<Country> options = new ArrayList<>();
        options.add(randomCountry);

        for (Country country : allCountries) {
            if (!country.getAlphaCode().equals(randomCountry.getAlphaCode()) && options.size() < 4) {
                options.add(country);
            }
        }

        Collections.shuffle(options);

        m.addAttribute("countryName", randomCountry.getName());
        m.addAttribute("countryFlag", randomCountry.getFlag());
        m.addAttribute("countryCode", randomCountry.getAlphaCode());
        m.addAttribute("correctCapital", randomCountry.getCapital());
        m.addAttribute("options", options);

        // Se è una sessione, salva il paese corrente
        String quizType = (String) session.getAttribute("quizType");
        if ("capital".equals(quizType)) {
            session.setAttribute("currentCountryCode", randomCountry.getAlphaCode());
        }

        addSessionStats(session, m);
        return "quiz_capital";
    }

    @PostMapping("quiz_capital_answer")
    public String processQuizCapitalAnswer(
            @RequestParam String countryCode,
            @RequestParam String correctAnswer,
            @RequestParam String selectedAnswer,
            HttpSession session,
            Model m) {

        List<Country> countries = service.getCountries();
        Country selectedCountry = countries.stream()
                .filter(c -> c.getAlphaCode().equals(countryCode))
                .findFirst()
                .orElse(null);

        boolean isCorrect = selectedAnswer.equals(correctAnswer);

        // TRACCIA LA RISPOSTA
        trackAnswer(session, isCorrect);

        // SALVA PUNTEGGIO SE UTENTE LOGGATO
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            saveQuizScore(auth.getName(), isCorrect ? 10 : 0, "Quiz Capital");
        }

        m.addAttribute("countryName", selectedCountry.getName());
        m.addAttribute("countryFlag", selectedCountry.getFlag());
        m.addAttribute("correctAnswer", correctAnswer);
        m.addAttribute("selectedAnswer", selectedAnswer);
        m.addAttribute("isCorrect", isCorrect);

        addSessionStats(session, m);
        return "quiz_capital_result";
    }

    @GetMapping("quiz_nation")
    public String getQuizNation(HttpSession session, Model m) {
        Country randomCountry = service.getRandomCountry();
        List<Country> allCountries = service.getCountries();
        Collections.shuffle(allCountries);

        List<Country> options = new ArrayList<>();
        options.add(randomCountry);

        for (Country country : allCountries) {
            if (!country.getAlphaCode().equals(randomCountry.getAlphaCode()) && options.size() < 4) {
                options.add(country);
            }
        }

        Collections.shuffle(options);

        m.addAttribute("capitalName", randomCountry.getCapital());
        m.addAttribute("countryCode", randomCountry.getAlphaCode());
        m.addAttribute("correctCountry", randomCountry.getName());
        m.addAttribute("options", options);

        // Se è una sessione, salva il paese corrente
        String quizType = (String) session.getAttribute("quizType");
        if ("nation".equals(quizType)) {
            Integer currentQ = (Integer) session.getAttribute("currentQuestionNumber");
            Integer sessionScore = (Integer) session.getAttribute("sessionScore");
            m.addAttribute("isSession", true);
            m.addAttribute("currentQuestionNumber", currentQ != null ? currentQ : 1);
            m.addAttribute("totalQuestions", 10);
            m.addAttribute("sessionScore", sessionScore != null ? sessionScore : 0);
            session.setAttribute("currentCountryCode", randomCountry.getAlphaCode());
        }

        addSessionStats(session, m);
        return "quiz_nation";
    }

    @PostMapping("quiz_nation_answer")
    public String processQuizNationAnswer(
            @RequestParam String countryCode,
            @RequestParam String correctAnswer,
            @RequestParam String selectedAnswer,
            HttpSession session,
            Model m) {

        List<Country> countries = service.getCountries();
        Country selectedCountry = countries.stream()
                .filter(c -> c.getAlphaCode().equals(countryCode))
                .findFirst()
                .orElse(null);

        boolean isCorrect = selectedAnswer.equals(correctAnswer);

        // TRACCIA LA RISPOSTA
        trackAnswer(session, isCorrect);

        // SALVA PUNTEGGIO SE UTENTE LOGGATO
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            saveQuizScore(auth.getName(), isCorrect ? 10 : 0, "Quiz Nation");
        }

        m.addAttribute("capitalName", selectedCountry.getCapital());
        m.addAttribute("countryFlag", selectedCountry.getFlag());
        m.addAttribute("correctAnswer", correctAnswer);
        m.addAttribute("selectedAnswer", selectedAnswer);
        m.addAttribute("isCorrect", isCorrect);

        addSessionStats(session, m);
        return "quiz_nation_result";
    }

    @GetMapping("quiz_flag")
    public String getQuizFlag(HttpSession session, Model m) {
        Country randomCountry = service.getRandomCountry();
        List<Country> allCountries = service.getCountries();
        Collections.shuffle(allCountries);

        List<Country> options = new ArrayList<>();
        options.add(randomCountry);

        for (Country country : allCountries) {
            if (!country.getAlphaCode().equals(randomCountry.getAlphaCode()) && options.size() < 4) {
                options.add(country);
            }
        }

        Collections.shuffle(options);

        m.addAttribute("countryFlag", randomCountry.getFlag());
        m.addAttribute("countryCode", randomCountry.getAlphaCode());
        m.addAttribute("correctCountry", randomCountry.getName());
        m.addAttribute("options", options);

        // Se è una sessione, salva il paese corrente
        String quizType = (String) session.getAttribute("quizType");
        if ("flag".equals(quizType)) {
            Integer currentQ = (Integer) session.getAttribute("currentQuestionNumber");
            Integer sessionScore = (Integer) session.getAttribute("sessionScore");
            m.addAttribute("isSession", true);
            m.addAttribute("currentQuestionNumber", currentQ != null ? currentQ : 1);
            m.addAttribute("totalQuestions", 10);
            m.addAttribute("sessionScore", sessionScore != null ? sessionScore : 0);
            session.setAttribute("currentCountryCode", randomCountry.getAlphaCode());
        }

        addSessionStats(session, m);
        return "quiz_flag";
    }

    @PostMapping("quiz_flag_answer")
    public String processQuizFlagAnswer(
            @RequestParam String countryCode,
            @RequestParam String correctAnswer,
            @RequestParam String selectedAnswer,
            HttpSession session,
            Model m) {

        List<Country> countries = service.getCountries();
        Country selectedCountry = countries.stream()
                .filter(c -> c.getAlphaCode().equals(countryCode))
                .findFirst()
                .orElse(null);

        boolean isCorrect = selectedAnswer.equals(correctAnswer);

        // TRACCIA LA RISPOSTA
        trackAnswer(session, isCorrect);

        // SALVA PUNTEGGIO SE UTENTE LOGGATO
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            saveQuizScore(auth.getName(), isCorrect ? 10 : 0, "Quiz Flag");
        }

        m.addAttribute("countryFlag", selectedCountry.getFlag());
        m.addAttribute("capitalName", selectedCountry.getCapital());
        m.addAttribute("regionName", selectedCountry.getRegion());
        m.addAttribute("correctAnswer", correctAnswer);
        m.addAttribute("selectedAnswer", selectedAnswer);
        m.addAttribute("isCorrect", isCorrect);

        addSessionStats(session, m);
        return "quiz_flag_result";
    }

    @GetMapping("quiz_continent")
    public String getQuizContinent(HttpSession session, Model m) {
        Country randomCountry = service.getRandomCountry();
        String regionFromDB = randomCountry.getRegion();

        m.addAttribute("countryName", randomCountry.getName());
        m.addAttribute("countryFlag", randomCountry.getFlag());
        m.addAttribute("capitalName", randomCountry.getCapital());
        m.addAttribute("countryCode", randomCountry.getAlphaCode());
        m.addAttribute("correctContinent", regionFromDB);

        // Se è una sessione, salva il paese corrente
        String quizType = (String) session.getAttribute("quizType");
        if ("continent".equals(quizType)) {
            Integer currentQ = (Integer) session.getAttribute("currentQuestionNumber");
            Integer sessionScore = (Integer) session.getAttribute("sessionScore");
            m.addAttribute("isSession", true);
            m.addAttribute("currentQuestionNumber", currentQ != null ? currentQ : 1);
            m.addAttribute("totalQuestions", 10);
            m.addAttribute("sessionScore", sessionScore != null ? sessionScore : 0);
            session.setAttribute("currentCountryCode", randomCountry.getAlphaCode());
        }

        addSessionStats(session, m);
        return "quiz_continent";
    }

    @PostMapping("quiz_continent_answer")
    public String processQuizContinentAnswer(
            @RequestParam String countryCode,
            @RequestParam String correctAnswer,
            @RequestParam String selectedAnswer,
            HttpSession session,
            Model m) {

        List<Country> countries = service.getCountries();
        Country selectedCountry = countries.stream()
                .filter(c -> c.getAlphaCode().equals(countryCode))
                .findFirst()
                .orElse(null);

        boolean isCorrect = selectedAnswer.equals(correctAnswer);

        // TRACCIA LA RISPOSTA
        trackAnswer(session, isCorrect);

        // SALVA PUNTEGGIO SE UTENTE LOGGATO
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            saveQuizScore(auth.getName(), isCorrect ? 10 : 0, "Quiz Continent");
        }

        m.addAttribute("countryName", selectedCountry.getName());
        m.addAttribute("countryFlag", selectedCountry.getFlag());
        m.addAttribute("capitalName", selectedCountry.getCapital());
        m.addAttribute("correctAnswer", correctAnswer);
        m.addAttribute("selectedAnswer", selectedAnswer);
        m.addAttribute("isCorrect", isCorrect);

        addSessionStats(session, m);
        return "quiz_continent_result";
    }

    // ===== CLASSE HELPER PER RISULTATI DETTAGLIATI =====

    public static class QuestionResult {
        private int questionNumber;
        private Country country;
        private String userAnswer;
        private String correctAnswer;
        private boolean correct;
        private String quizType;

        // Getters e Setters
        public int getQuestionNumber() {
            return questionNumber;
        }

        public void setQuestionNumber(int questionNumber) {
            this.questionNumber = questionNumber;
        }

        public Country getCountry() {
            return country;
        }

        public void setCountry(Country country) {
            this.country = country;
        }

        public String getUserAnswer() {
            return userAnswer;
        }

        public void setUserAnswer(String userAnswer) {
            this.userAnswer = userAnswer;
        }

        public String getCorrectAnswer() {
            return correctAnswer;
        }

        public void setCorrectAnswer(String correctAnswer) {
            this.correctAnswer = correctAnswer;
        }

        public boolean isCorrect() {
            return correct;
        }

        public void setCorrect(boolean correct) {
            this.correct = correct;
        }

        public String getQuizType() {
            return quizType;
        }

        public void setQuizType(String quizType) {
            this.quizType = quizType;
        }
    }

    @GetMapping("higher_lower_session")
    public String startHigherLowerSession(HttpSession session, Model m) {
        session.setAttribute("quizType", "higher_lower");
        session.setAttribute("currentQuestionNumber", 1);
        session.setAttribute("sessionScore", 0);
        session.setAttribute("sessionAnswers", new ArrayList<Boolean>());
        session.setAttribute("userAnswers", new ArrayList<String>());
        session.setAttribute("correctAnswersList", new ArrayList<String>());
        session.setAttribute("questionCountries", new ArrayList<Country>());

        m.addAttribute("isSession", true);
        m.addAttribute("currentQuestionNumber", 1);
        m.addAttribute("totalQuestions", 10);
        m.addAttribute("sessionScore", 0);

        return getHigherLowerQuestion(session, m);
    }

    @PostMapping("higher_lower_session_answer")
    public String processHigherLowerSessionAnswer(
            @RequestParam String country1Code,
            @RequestParam String country2Code,
            @RequestParam String population1,
            @RequestParam String population2,
            @RequestParam String selectedCountry,
            HttpSession session,
            Model m) {

        String quizType = (String) session.getAttribute("quizType");
        if (!"higher_lower".equals(quizType)) {
            return processHigherLowerAnswer(country1Code, country2Code, population1, population2, selectedCountry,
                    session, m);
        }

        // Recupera i paesi
        List<Country> countries = service.getCountries();
        Country country1 = countries.stream()
                .filter(c -> c.getAlphaCode().equals(country1Code))
                .findFirst()
                .orElse(null);
        Country country2 = countries.stream()
                .filter(c -> c.getAlphaCode().equals(country2Code))
                .findFirst()
                .orElse(null);

        long pop1 = Long.parseLong(population1);
        long pop2 = Long.parseLong(population2);

        // Determina la risposta corretta
        boolean country1HasMore = pop1 > pop2;
        String correctCountryCode = country1HasMore ? country1Code : country2Code;
        String correctCountryName = country1HasMore ? country1.getName() : country2.getName();

        // Controlla se l'utente ha scelto correttamente
        boolean isCorrect = selectedCountry.equals(correctCountryCode);
        

        return processSessionAnswer("higher_lower", correctCountryName,
                selectedCountry.equals(country1Code) ? country1.getName() : country2.getName(),
                session, m, () -> {
                    return getHigherLowerQuestion(session, m);
                });
    }

    // Metodo helper per generare una domanda Higher or Lower
    private String getHigherLowerQuestion(HttpSession session, Model m) {
        Country country1 = service.getRandomCountry();
        Country country2 = service.getRandomCountry();

        // Assicurati che siano paesi diversi
        while (country2.getAlphaCode().equals(country1.getAlphaCode())) {
            country2 = service.getRandomCountry();
        }

        m.addAttribute("country1", country1);
        m.addAttribute("country2", country2);

        // Salva i paesi correnti nella sessione
        session.setAttribute("currentCountry1Code", country1.getAlphaCode());
        session.setAttribute("currentCountry2Code", country2.getAlphaCode());

        return "higher_lower";
    }
}