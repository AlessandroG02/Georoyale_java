package com.georoyale.rapelli.georoyale.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.georoyale.rapelli.georoyale.entities.Country;
import com.georoyale.rapelli.georoyale.services.CountryService;

import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("api")
public class CountryRest {

    @Autowired
    private CountryService service;

    @GetMapping("countries")
    public ResponseEntity<List<Country>> getCountries() {
        return new ResponseEntity<>(service.getCountries(), HttpStatus.OK);
    }


}
