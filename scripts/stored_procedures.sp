\connect osyris

DROP FUNCTION store_tmp_wf_result(character varying) ;
CREATE FUNCTION store_tmp_wf_result(character varying) 
				RETURNS character varying AS

$BODY$

	DECLARE
	
		--declare alias variables for parameters
		wfid ALIAS FOR $1;
		tmp_result integer default 1;
		pos integer default 0;
		res text default NULL;
	BEGIN
		LOCK TABLE workflows IN ACCESS EXCLUSIVE MODE;
		SELECT position('warning' in result) INTO pos FROM workflows WHERE id=wfid;
		IF (pos > 0) THEN
			tmp_result = 1;
		ELSE
			SELECT result INTO res FROM workflows WHERE id=wfid;
			IF (res = '' or res IS NULL) THEN
				tmp_result = 1;
			ELSE
				SELECT cast(result as int)+1 INTO tmp_result FROM workflows WHERE id=wfid;
			END IF;		
		END IF;	
		UPDATE workflows SET result=tmp_result WHERE id=wfid;
		RETURN tmp_result;
	END;
$BODY$
LANGUAGE 'plpgsql' VOLATILE;
select store_tmp_wf_result('2e71b66f-4ade-4269-8a53-6a0e32b4fe5b');
--select store_tmp_wf_result('17146040-bd47-4cd6-a151-420908d38bf7') INTO result;

