SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL';

DROP TABLE IF EXISTS ais_class_a_static;
DROP TABLE IF EXISTS ais_vessel_static;
DROP TABLE IF EXISTS ais_class_a_position;
DROP TABLE IF EXISTS ais_vessel_position;
DROP TABLE IF EXISTS ais_pos_message;
DROP TABLE IF EXISTS ais_message;
DROP TABLE IF EXISTS ais_vessel_target;
DROP TABLE IF EXISTS ais_vessel_track;

-- Vessel target
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
) ENGINE = myISAM;

-- Raw AIS message table
CREATE TABLE ais_message (
	id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	mmsi INT NOT NULL,
	msg_id TINYINT NOT NULL,
	message VARCHAR(512) NOT NULL,
	source_timestamp DATETIME NULL,
	received DATETIME NOT NULL,
	created DATETIME NOT NULL
) ENGINE = myISAM;

-- AIS position message information
CREATE TABLE ais_pos_message (
	ais_message INT NOT NULL PRIMARY KEY,
	lat DOUBLE NULL,
	lon DOUBLE NULL,
	INDEX(lat, lon),
	FOREIGN KEY (ais_message) REFERENCES ais_message(id)
) ENGINE = myISAM;
	
-- Current vessel target position
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
) ENGINE = myISAM;

-- Vessels track
CREATE TABLE ais_vessel_track (
	id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	mmsi INT NOT NULL,
	lat DOUBLE NOT NULL,
	lon DOUBLE NOT NULL,
	sog DOUBLE NOT NULL,
	cog DOUBLE NOT NULL,
	time DATETIME NOT NULL,
	valid_to DATETIME NOT NULL,
	created DATETIME NOT NULL,
	INDEX(mmsi, time),
	INDEX(valid_to)
) ENGINE = myISAM;

-- Extended class A position information
CREATE TABLE ais_class_a_position (
	mmsi INT NOT NULL PRIMARY KEY,
	nav_status TINYINT NOT NULL,
	rot DOUBLE NULL,	
	special_man_indicator TINYINT NOT NULL,
	FOREIGN KEY (mmsi) REFERENCES ais_vessel_position(mmsi)
) ENGINE = myISAM;

-- Current vessel target statics
CREATE TABLE ais_vessel_static (
	mmsi INT NOT NULL PRIMARY KEY,
	name VARCHAR(32) NULL,
	callsign VARCHAR(8) NULL,
	ship_type TINYINT NULL,
	decoded_ship_type TINYINT NULL,
	cargo TINYINT NULL,
	dim_bow SMALLINT NULL,
	dim_stern SMALLINT NULL,
	dim_port TINYINT NULL,
	dim_starboard TINYINT NULL,
	received DATETIME NOT NULL,
	created DATETIME NOT NULL,
	FOREIGN KEY (mmsi) REFERENCES ais_vessel_target(mmsi)
) ENGINE = myISAM;

-- Extended class A statics
CREATE TABLE ais_class_a_static (
	mmsi INT NOT NULL PRIMARY KEY,
	version TINYINT NOT NULL,
	imo INT NULL,
	pos_type TINYINT NOT NULL,
	eta DATETIME NULL,
	draught SMALLINT NOT NULL,
	destination VARCHAR(32) NULL,
	dte TINYINT NOT NULL,
	FOREIGN KEY (mmsi) REFERENCES ais_vessel_static(mmsi)
) ENGINE = myISAM;

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
