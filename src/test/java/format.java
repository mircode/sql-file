public class format {
	public String format(String row,String field,String fs){
		String[] rows=row.split("\\|");
		String[] fields=fs.split(",");
		for(int i=0,len=fields.length;i<len;i++){
			if(fields[i].equals(field)) return rows[i];
		}
		return null;
	}
}
