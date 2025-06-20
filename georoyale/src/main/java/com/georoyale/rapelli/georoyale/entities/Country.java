package com.georoyale.rapelli.georoyale.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="countries")
public class Country {

	@Id
	@Column(name = "alpha2Code")
	private String alphaCode;
	
	private String name;
	private String capital;
	private String region;
	
	// Campo per Higher or Lower - DEVE esistere nel database
	private Long population;
	
	@Transient 
	private String flag;

	
	public String getFlag() {
		return "/flags/" + alphaCode.toLowerCase() + ".png";
	}
}