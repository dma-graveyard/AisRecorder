package dk.frv.aisrecorder.persistence.mapper;

import java.util.Date;

import dk.frv.aisrecorder.persistence.domain.AisVesselTrack;

public interface AisVesselTrackMapper {
    /**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table ais_vessel_track
	 * @mbggenerated  Thu Dec 08 11:32:48 CET 2011
	 */
	int deleteByPrimaryKey(Integer id);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table ais_vessel_track
	 * @mbggenerated  Thu Dec 08 11:32:48 CET 2011
	 */
	int insert(AisVesselTrack record);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table ais_vessel_track
	 * @mbggenerated  Thu Dec 08 11:32:48 CET 2011
	 */
	int insertSelective(AisVesselTrack record);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table ais_vessel_track
	 * @mbggenerated  Thu Dec 08 11:32:48 CET 2011
	 */
	AisVesselTrack selectByPrimaryKey(Integer id);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table ais_vessel_track
	 * @mbggenerated  Thu Dec 08 11:32:48 CET 2011
	 */
	int updateByPrimaryKeySelective(AisVesselTrack record);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table ais_vessel_track
	 * @mbggenerated  Thu Dec 08 11:32:48 CET 2011
	 */
	int updateByPrimaryKey(AisVesselTrack record);

	int deleteOld(Date from);    
}