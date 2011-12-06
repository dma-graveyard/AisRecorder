package dk.frv.aisrecorder.entities;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * The persistent class for the ais_vessel_target database table.
 * 
 */
@Entity
@Table(name = "ais_vessel_target")
public class AisVesselTarget implements Serializable {
	private static final long serialVersionUID = 1L;

	private int mmsi;
	private int id;
	private Date created;
	private Date lastReceived;
	private Date validTo;
	private String vesselClass;
	private String country;
	private String source;
	private AisVesselPosition aisVesselPosition;
	private AisVesselStatic aisVesselStatic;

	public AisVesselTarget() {
		this.created = new Date();
	}

	@Id
	@Column(unique = true, nullable = false)
	public int getMmsi() {
		return this.mmsi;
	}

	public void setMmsi(int mmsi) {
		this.mmsi = mmsi;
	}
	
	@GeneratedValue
	@Column(unique = true, nullable = false, updatable = false)
	public int getId() {
		return this.id;
	}
	
	public void setId(int id) {
		this.id = id;
	}

	@Column(nullable = false)
	public Date getCreated() {
		return this.created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	@Column(name = "last_received", nullable = false)
	public Date getLastReceived() {
		return this.lastReceived;
	}

	public void setLastReceived(Date lastReceived) {
		this.lastReceived = lastReceived;
	}
	
	@Column(name = "valid_to", nullable = false)
	public Date getValidTo() {
		return validTo;
	}
	
	public void setValidTo(Date validTo) {
		this.validTo = validTo;
	}

	@Column(name = "vessel_class", nullable = false, length = 1)
	public String getVesselClass() {
		return this.vesselClass;
	}

	public void setVesselClass(String vesselClass) {
		this.vesselClass = vesselClass;
	}
	
	@Column(name = "country", nullable = true, length = 3)
	public String getCountry() {
		return country;
	}
	
	public void setCountry(String country) {
		this.country = country;
	}
	
	@Column(name = "source", nullable = false, length = 4)
	public String getSource() {
		return source;
	}
	
	public void setSource(String source) {
		this.source = source;
	}

	// bi-directional one-to-one association to AisVesselPosition
	@OneToOne(mappedBy = "aisVesselTarget")
	public AisVesselPosition getAisVesselPosition() {
		return this.aisVesselPosition;
	}

	public void setAisVesselPosition(AisVesselPosition aisVesselPosition) {
		this.aisVesselPosition = aisVesselPosition;
	}

	// bi-directional one-to-one association to AisVesselStatic
	@OneToOne(mappedBy = "aisVesselTarget")
	public AisVesselStatic getAisVesselStatic() {
		return this.aisVesselStatic;
	}

	public void setAisVesselStatic(AisVesselStatic aisVesselStatic) {
		this.aisVesselStatic = aisVesselStatic;
	}

}