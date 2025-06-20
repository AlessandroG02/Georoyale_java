package com.georoyale.rapelli.georoyale.services;

import java.util.List;

import com.georoyale.rapelli.georoyale.entities.Country;

public interface CountryService {

    List<Country> getCountries();
    Country getRandomCountry();
}
