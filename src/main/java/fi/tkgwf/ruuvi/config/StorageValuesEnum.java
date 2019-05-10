package fi.tkgwf.ruuvi.config;

public enum StorageValuesEnum  {

    RAW("raw"),
    EXTENDED("extended"),
    WHITELIST("whitelist"),
    BLACKLIST("blacklist"),
    NAMED("named");

    StorageValuesEnum(String method) {
        this.method = method;
    }

    private String method;

    /**
     * @return the method
     */
    String getMethod() {
        return method;
    }

	public static StorageValuesEnum resolve(String storageValueProp) {
        for (StorageValuesEnum val: StorageValuesEnum.values()) {
            if (val.method == storageValueProp) {
                return val;
            }
        }
		return null;
	}

}