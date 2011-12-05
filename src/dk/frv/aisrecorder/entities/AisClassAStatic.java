package dk.frv.aisrecorder.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.util.Date;

/**
 * The persistent class for the ais_class_a_static database table.
 * 
 */
@Entity
@Table(name = "ais_class_a_static")
public class AisClassAStatic implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private int mmsi;
	private String distination;
	private short draught;
	private byte dte;
	private Date eta;
	private int imo;
	private byte posType;
	private byte version;
	private AisVesselStatic aisVesselStatic;

	public AisClassAStatic() {
	}

	@Id
	@Column(unique = true, nullable = false)
	public int getMmsi() {
		return this.mmsi;
	}

	public void setMmsi(int mmsi) {
		this.mmsi = mmsi;
	}

	@Column(length = 32)
	public String getDistination() {
		return this.distination;
	}

	public void setDistination(String distination) {
		this.distination = distination;
	}

	@Column(nullable = false)
	public short getDraught() {
		return this.draught;
	}

	public void setDraught(short draught) {
		this.draught = draught;
	}

	@Column(nullable = false)
	public byte getDte() {
		return this.dte;
	}

	public void setDte(byte dte) {
		this.dte = dte;
	}

	public Date getEta() {
		return this.eta;
	}

	public void setEta(Date eta) {
		this.eta = eta;
	}

	public int getImo() {
		return this.imo;
	}

	public void setImo(int imo) {
		this.imo = imo;
	}

	@Column(name = "pos_type", nullable = false)
	public byte getPosType() {
		return this.posType;
	}

	public void setPosType(byte posType) {
		this.posType = posType;
	}

	@Column(nullable = false)
	public byte getVersion() {
		return this.version;
	}

	public void setVersion(byte version) {
		this.version = version;
	}

	// bi-directional one-to-one association to AisVesselStatic
	@OneToOne
	@JoinColumn(name = "mmsi", nullable = false, insertable = false, updatable = false)
	public AisVesselStatic getAisVesselStatic() {
		return this.aisVesselStatic;
	}

	public void setAisVesselStatic(AisVesselStatic aisVesselStatic) {
		this.aisVesselStatic = aisVesselStatic;
	}

}