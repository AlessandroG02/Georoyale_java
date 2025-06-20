package com.georoyale.rapelli.georoyale.repos;

import org.springframework.data.jpa.repository.JpaRepository;

import com.georoyale.rapelli.georoyale.entities.Country;

public interface CountryRepo extends JpaRepository<Country, String>{

}
