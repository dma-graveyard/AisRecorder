package dk.frv.aisrecorder.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.util.Date;

/**
 * The persistent class for the ais_vessel_target database table.
 * 
 */
@Entity
@Table(name = "ais_vessel_target")
public class AisVesselTarget implements Serializable {
	private static final long serialVersionUID = 1L;

	private int mmsi;
	private Date created;
	private Date lastReceived;
	private String vesselClass;
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

	@Column(nullable = false)
	public Date getCreated() {
		return this.created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	@Column(name = "last_received")
	public Date getLastReceived() {
		return this.lastReceived;
	}

	public void setLastReceived(Date lastReceived) {
		this.lastReceived = lastReceived;
	}

	@Column(name = "vessel_class", nullable = false, length = 1)
	public String getVesselClass() {
		return this.vesselClass;
	}

	public void setVesselClass(String vesselClass) {
		this.vesselClass = vesselClass;
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