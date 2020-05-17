package cn.jmicro.api.mng;

import cn.jmicro.api.annotation.SO;

@SO
public class ReportData {

	private Short[] types;
	
	private String[] labels;
	
	private Double[] qps;
	private Long[] total;
	private Double[] percent;
	private Long[] cur;
	private Double[] curPercent;
	
	public Short[] getTypes() {
		return types;
	}

	public void setTypes(Short[] types) {
		this.types = types;
	}

	public String[] getLabels() {
		return labels;
	}

	public void setLabels(String[] labels) {
		this.labels = labels;
	}

	public Double[] getQps() {
		return qps;
	}

	public void setQps(Double[] qps) {
		this.qps = qps;
	}

	public Long[] getCur() {
		return cur;
	}

	public void setCur(Long[] cur) {
		this.cur = cur;
	}

	public Long[] getTotal() {
		return total;
	}

	public void setTotal(Long[] total) {
		this.total = total;
	}

	public Double[] getPercent() {
		return percent;
	}

	public void setPercent(Double[] percent) {
		this.percent = percent;
	}

	public Double[] getCurPercent() {
		return curPercent;
	}

	public void setCurPercent(Double[] curPercent) {
		this.curPercent = curPercent;
	}

	
}
