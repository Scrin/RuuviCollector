// Copyright 2021 Siemens Mobility GmbH

package fi.tkgwf.ruuvi.bean;

public class EddystoneTLM {

	private Double vBatt;

	private Double temp;

	private Integer advCnt;

	private Integer secCnt;

	public Double getVBatt() {
		return vBatt;
	}

	public void setVBatt(Double vBatt) {
		this.vBatt = vBatt;
	}

	public Double getTemp() {
		return temp;
	}

	public void setTemp(Double temp) {
		this.temp = temp;
	}

	public Integer getAdvCnt() {
		return advCnt;
	}

	public void setAdvCnt(Integer advCnt) {
		this.advCnt = advCnt;
	}

	public Integer getSecCnt() {
		return secCnt;
	}

	public void setSecCnt(Integer secCnt) {
		this.secCnt = secCnt;
	}
}
