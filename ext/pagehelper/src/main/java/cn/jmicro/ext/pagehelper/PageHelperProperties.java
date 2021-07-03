package cn.jmicro.ext.pagehelper;

import java.util.Properties;


public class PageHelperProperties {

    public static final String PAGEHELPER_PREFIX = "/pagehelper/*";

   // @Cfg(value=PageHelperProperties.PAGEHELPER_PREFIX)
    private Properties pagehelper = new Properties();

    public Properties getProperties() {
        return pagehelper;
    }

    public String getOffsetAsPageNum() {
        return pagehelper.getProperty("offsetAsPageNum");
    }

    public void setOffsetAsPageNum(String offsetAsPageNum) {
        pagehelper.setProperty("offsetAsPageNum", offsetAsPageNum);
    }

    public String getRowBoundsWithCount() {
        return pagehelper.getProperty("rowBoundsWithCount");
    }

    public void setRowBoundsWithCount(String rowBoundsWithCount) {
        pagehelper.setProperty("rowBoundsWithCount", rowBoundsWithCount);
    }

    public String getPageSizeZero() {
        return pagehelper.getProperty("pageSizeZero");
    }

    public void setPageSizeZero(String pageSizeZero) {
        pagehelper.setProperty("pageSizeZero", pageSizeZero);
    }

    public String getReasonable() {
        return pagehelper.getProperty("reasonable");
    }

    public void setReasonable(String reasonable) {
        pagehelper.setProperty("reasonable", reasonable);
    }

    public String getSupportMethodsArguments() {
        return pagehelper.getProperty("supportMethodsArguments");
    }

    public void setSupportMethodsArguments(String supportMethodsArguments) {
        pagehelper.setProperty("supportMethodsArguments", supportMethodsArguments);
    }

    public String getDialect() {
        return pagehelper.getProperty("dialect");
    }

    public void setDialect(String dialect) {
        pagehelper.setProperty("dialect", dialect);
    }

    public String getHelperDialect() {
        return pagehelper.getProperty("helperDialect");
    }

    public void setHelperDialect(String helperDialect) {
        pagehelper.setProperty("helperDialect", helperDialect);
    }

    public String getAutoRuntimeDialect() {
        return pagehelper.getProperty("autoRuntimeDialect");
    }

    public void setAutoRuntimeDialect(String autoRuntimeDialect) {
        pagehelper.setProperty("autoRuntimeDialect", autoRuntimeDialect);
    }

    public String getAutoDialect() {
        return pagehelper.getProperty("autoDialect");
    }

    public void setAutoDialect(String autoDialect) {
        pagehelper.setProperty("autoDialect", autoDialect);
    }

    public String getCloseConn() {
        return pagehelper.getProperty("closeConn");
    }

    public void setCloseConn(String closeConn) {
        pagehelper.setProperty("closeConn", closeConn);
    }

    public String getParams() {
        return pagehelper.getProperty("params");
    }

    public void setParams(String params) {
        pagehelper.setProperty("params", params);
    }

    public String getDefaultCount() {
        return pagehelper.getProperty("defaultCount");
    }

    public void setDefaultCount(String defaultCount) {
        pagehelper.setProperty("defaultCount", defaultCount);
    }

    public String getDialectAlias() {
        return pagehelper.getProperty("dialectAlias");
    }

    public void setDialectAlias(String dialectAlias) {
        pagehelper.setProperty("dialectAlias", dialectAlias);
    }

    
}
