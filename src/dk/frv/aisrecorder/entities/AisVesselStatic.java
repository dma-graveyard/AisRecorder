package dk.frv.aisrecorder.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.util.Date;

/**
 * The persistent class for the ais_vessel_static database table.
 * 
 */
@Entity
@Table(name = "ais_vessel_static")
public class AisVesselStatic implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private int mmsi;
	private String callsign;
	private Date created;
	private short dimBow;
	private byte dimPort;
	private byte dimStarboard;
	private short dimStern;
	private String name;
	private Date received;
	private byte shipType;
	private AisClassAStatic aisClassAStatic;
	private AisVesselTarget aisVesselTarget;

	public AisVesselStatic() {
		
	}

	@Id
	@Column(unique = true, nullable = false)
	public int getMmsi() {
		return this.mmsi;
	}

	public void setMmsi(int mmsi) {
		this.mmsi = mmsi;
	}

	@Column(nullable = false, length = 8)
	public String getCallsign() {
		return this.callsign;
	}

	public void setCallsign(String callsign) {
		this.callsign = callsign;
	}

	@Column(nullable = false)
	public Date getCreated() {
		return this.created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	@Column(name = "dim_bow", nullable = false)
	public short getDimBow() {
		return this.dimBow;
	}

	public void setDimBow(short dimBow) {
		this.dimBow = dimBow;
	}

	@Column(name = "dim_port", nullable = false)
	public byte getDimPort() {
		return this.dimPort;
	}

	public void setDimPort(byte dimPort) {
		this.dimPort = dimPort;
	}

	@Column(name = "dim_starboard", nullable = false)
	public byte getDimStarboard() {
		return this.dimStarboard;
	}

	public void setDimStarboard(byte dimStarboard) {
		this.dimStarboard = dimStarboard;
	}

	@Column(name = "dim_stern", nullable = false)
	public short getDimStern() {
		return this.dimStern;
	}

	public void setDimStern(short dimStern) {
		this.dimStern = dimStern;
	}

	@Column(nullable = false, length = 32)
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(nullable = false)
	public Date getReceived() {
		return this.received;
	}

	public void setReceived(Date received) {
		this.received = received;
	}

	@Column(name = "ship_type", nullable = false)
	public byte getShipType() {
		return this.shipType;
	}

	public void setShipType(byte shipType) {
		this.shipType = shipType;
	}

	// bi-directional one-to-one association to AisClassAStatic
	@OneToOne(mappedBy = "aisVesselStatic")
	public AisClassAStatic getAisClassAStatic() {
		return this.aisClassAStatic;
	}

	public void setAisClassAStatic(AisClassAStatic aisClassAStatic) {
		this.aisClassAStatic = aisClassAStatic;
	}

	// bi-directional one-to-one association to AisVesselTarget
	@OneToOne
	@JoinColumn(name = "mmsi", nullable = false, insertable = false, updatable = false)
	public AisVesselTarget getAisVesselTarget() {
		return this.aisVesselTarget;
	}

	public void setAisVesselTarget(AisVesselTarget aisVesselTarget) {
		this.aisVesselTarget = aisVesselTarget;
	}

}