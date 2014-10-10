package attlabs;

public class User {

	private String oldUserId = null;
	private String newUserId = null;
	private boolean migrated = false;
	
	public User(){
		
	}

	private String removeSpecialCharacters(String userId, int type) {
    	
    	String result = userId.replaceAll("[	!#$%^&*:,+ ]","");
    	if (result.length()!=userId.length()) {
    		if (type==0)
    		System.out.println("Old Userid |" + userId + "| contains special characters. Removing special characters.... proceed normally");
    		if (type==1)
    		//System.out.println(result);
    		System.out.println("New Userid |" + userId + "| contains special characters!. Removing special characters.... proceed normally");
    	}
    	return result;
    }

	public boolean isValid() {
		if (oldUserId.length()>0 && newUserId.length()>0) {
			//this.oldUserId = removeSpecialCharacters(oldUserId,0);
			this.newUserId = removeSpecialCharacters(newUserId,1);
			return true;
		}
		return false;
	}
	
	public String getOldUserId() {
		return oldUserId;
	}

	public void setOldUserId(String oldUserId) {
		this.oldUserId = oldUserId;
	}

	public String getNewUserId() {
		return newUserId;
	}

	public void setNewUserId(String newUserId) {
		this.newUserId = newUserId;
	}

	public boolean isMigrated() {
		return migrated;
	}

	public void setMigrated(boolean migrated) {
		this.migrated = migrated;
	}
	
	
	
}
