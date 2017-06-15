function format(row,field,fields){ 
	var fields=fields.split(','); 
	for(var i in fields){ 
		if(fields[i]==field){ 
			return row.split('|')[i];
		}
	}
	return null;
};