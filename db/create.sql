SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL';

-- Vessel target
DROP TABLE IF EXISTS ais_vessel_target;
CREATE TABLE ais_vessel_target (
	mmsi INT NOT NULL PRIMARY KEY,
	id INT NOT NULL AUTO_INCREMENT,
	vessel_class ENUM('A','B') NOT NULL DEFAULT 'A',
	country CHAR(3) NULL,
	source VARCHAR(4) NOT NULL DEFAULT 'LIVE',
	last_received DATETIME NOT NULL,
	valid_to DATETIME NOT NULL,
	created DATETIME NOT NULL,
	INDEX(id),
	INDEX(country),
	INDEX(valid_to)
) ENGINE = innoDB;

-- Raw AIS message table
DROP TABLE IF EXISTS ais_message;
CREATE TABLE ais_message (
	id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	mmsi INT NOT NULL,
	msg_id TINYINT NOT NULL,
	message VARCHAR(512) NOT NULL,
	source_timestamp DATETIME NULL,
	received DATETIME NOT NULL,
	created DATETIME NOT NULL
) ENGINE = innoDB;

-- AIS position message information
DROP TABLE IF EXISTS ais_pos_message;
CREATE TABLE ais_pos_message (
	ais_message INT NOT NULL PRIMARY KEY,
	lat DOUBLE NULL,
	lon DOUBLE NULL,
	INDEX(lat, lon),
	FOREIGN KEY (ais_message) REFERENCES ais_message(id)
) ENGINE = innoDB;
	
-- Current vessel target position
DROP TABLE IF EXISTS ais_vessel_position;
CREATE TABLE ais_vessel_position (
	mmsi INT NOT NULL PRIMARY KEY,
	lat DOUBLE NULL,
	lon DOUBLE NULL,
	pos_acc TINYINT NOT NULL,
	sog DOUBLE NULL,
	cog DOUBLE NULL,
	heading DOUBLE NULL,
	raim TINYINT NOT NULL,
	utc_sec TINYINT NOT NULL,
	source_timestamp DATETIME NULL,
	received DATETIME NOT NULL,
	created DATETIME NOT NULL,
	INDEX(lat,lon),
	INDEX(received),
	FOREIGN KEY (mmsi) REFERENCES ais_vessel_target(mmsi)
) ENGINE = innoDB;

-- Extended class A position information
DROP TABLE IF EXISTS ais_class_a_position;
CREATE TABLE ais_class_a_position (
	mmsi INT NOT NULL PRIMARY KEY,
	nav_status TINYINT NOT NULL,
	rot DOUBLE NULL,	
	special_man_indicator TINYINT NOT NULL,
	FOREIGN KEY (mmsi) REFERENCES ais_vessel_position(mmsi)
) ENGINE = innoDB;

-- Current vessel target statics
DROP TABLE IF EXISTS ais_vessel_static;
CREATE TABLE ais_vessel_static (
	mmsi INT NOT NULL PRIMARY KEY,
	name VARCHAR(32) NOT NULL,
	callsign VARCHAR(8) NOT NULL,
	ship_type TINYINT NOT NULL,
	dim_bow SMALLINT NOT NULL,
	dim_stern SMALLINT NOT NULL,
	dim_port TINYINT NOT NULL,
	dim_starboard TINYINT NOT NULL,
	received DATETIME NOT NULL,
	created DATETIME NOT NULL,
	FOREIGN KEY (mmsi) REFERENCES ais_vessel_target(mmsi)
) ENGINE = innoDB;

-- Extended class A statics
DROP TABLE IF EXISTS ais_class_a_static;
CREATE TABLE ais_class_a_static (
	mmsi INT NOT NULL PRIMARY KEY,
	version TINYINT NOT NULL,
	imo INT NULL,
	pos_type TINYINT NOT NULL,
	eta DATETIME NULL,
	draught SMALLINT NOT NULL,
	distination VARCHAR(32) NULL,
	dte TINYINT NOT NULL,
	FOREIGN KEY (mmsi) REFERENCES ais_vessel_static(mmsi)
) ENGINE = innoDB;

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
