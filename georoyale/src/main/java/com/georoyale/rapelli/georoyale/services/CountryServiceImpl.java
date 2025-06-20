package com.georoyale.rapelli.georoyale.services;

import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.georoyale.rapelli.georoyale.entities.Country;
import com.georoyale.rapelli.georoyale.repos.CountryRepo;

@Service
public class CountryServiceImpl implements CountryService {

    @Autowired
    private CountryRepo repo;

    @Override
    public List<Country> getCountries() {
        return repo.findAll();
    }

    @Override
    public Country getRandomCountry() {
        List<Country> listaPaesi = repo.findAll();
        
        // CONTROLLO SICUREZZA - Lista vuota
        if (listaPaesi == null || listaPaesi.isEmpty()) {
            throw new RuntimeException("Nessun paese trovato nel database! Controlla la tabella 'countries'.");
        }
        
        Random random = new Random();
        int indiceCasuale = random.nextInt(listaPaesi.size());
        return listaPaesi.get(indiceCasuale);    
    }
}