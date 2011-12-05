package dk.frv.aisrecorder.entities;

import java.io.Serializable;
import javax.persistence.*;

import java.util.Date;

/**
 * The persistent class for the ais_vessel_position database table.
 * 
 */
@Entity
@Table(name = "ais_vessel_position")
public class AisVesselPosition implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private int mmsi;
	private Double cog;
	private Date created;
	private Double heading = null;
	private Double lat = null;
	private Double lon = null;
	private byte posAcc;
	private byte utcSec;
	private byte raim;
	private Date received;
	private Double sog = null;
	private Date sourceTimestamp;
	private AisClassAPosition aisClassAPosition;
	private AisVesselTarget aisVesselTarget;

	public AisVesselPosition() {
		
	}

	@Id
	@Column(unique = true, nullable = false)
	public int getMmsi() {
		return this.mmsi;
	}

	public void setMmsi(int mmsi) {
		this.mmsi = mmsi;
	}

	public Double getCog() {
		return this.cog;
	}

	public void setCog(Double cog) {
		this.cog = cog;
	}

	@Column(nullable = false)
	public Date getCreated() {
		return this.created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Double getHeading() {
		return this.heading;
	}

	public void setHeading(Double heading) {
		this.heading = heading;
	}

	public Double getLat() {
		return this.lat;
	}

	public void setLat(Double lat) {
		this.lat = lat;
	}

	public Double getLon() {
		return this.lon;
	}

	public void setLon(Double lon) {
		this.lon = lon;
	}

	@Column(name = "pos_acc", nullable = false)
	public byte getPosAcc() {
		return this.posAcc;
	}

	public void setPosAcc(byte posAcc) {
		this.posAcc = posAcc;
	}

	@Column(nullable = false)
	public byte getRaim() {
		return this.raim;
	}

	public void setRaim(byte raim) {
		this.raim = raim;
	}
	
	@Column(name = "utc_sec", nullable = false)
	public byte getUtcSec() {
		return this.utcSec;
	}

	public void setUtcSec(byte utcSec) {
		this.utcSec = utcSec;
	}

	@Column(nullable = false)
	public Date getReceived() {
		return this.received;
	}

	public void setReceived(Date received) {
		this.received = received;
	}

	public Double getSog() {
		return this.sog;
	}

	public void setSog(Double sog) {
		this.sog = sog;
	}

	@Column(name = "source_timestamp")
	public Date getSourceTimestamp() {
		return this.sourceTimestamp;
	}

	public void setSourceTimestamp(Date sourceTimestamp) {
		this.sourceTimestamp = sourceTimestamp;
	}

	// bi-directional one-to-one association to AisClassAPosition
	@OneToOne(mappedBy = "aisVesselPosition")
	public AisClassAPosition getAisClassAPosition() {
		return this.aisClassAPosition;
	}

	public void setAisClassAPosition(AisClassAPosition aisClassAPosition) {
		this.aisClassAPosition = aisClassAPosition;
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