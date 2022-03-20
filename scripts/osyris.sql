--
-- PostgreSQL database dump
--

-- Started on 2009-02-22 13:12:16

SET client_encoding = 'SQL_ASCII';
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;

--
-- TOC entry 1651 (class 1262 OID 24605)
-- Name: osyris; Type: DATABASE; Schema: -; Owner: postgres
--

--CREATE DATABASE osyris WITH TEMPLATE = template0 ENCODING = 'SQL_ASCII';


ALTER DATABASE osyris OWNER TO postgres;

\connect osyris

SET client_encoding = 'SQL_ASCII';
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;

--
-- TOC entry 1652 (class 0 OID 0)
-- Dependencies: 4
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON SCHEMA public IS 'Standard public schema';


--
-- TOC entry 284 (class 2612 OID 16386)
-- Name: plpgsql; Type: PROCEDURAL LANGUAGE; Schema: -; Owner: postgres
--

CREATE PROCEDURAL LANGUAGE plpgsql;


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 1293 (class 1259 OID 24614)
-- Dependencies: 4
-- Name: locations; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE locations (
    id integer NOT NULL,
    name character varying(255) NOT NULL
);


ALTER TABLE public.locations OWNER TO postgres;

--
-- TOC entry 1294 (class 1259 OID 24616)
-- Dependencies: 1293 4
-- Name: locations_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE locations_id_seq
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.locations_id_seq OWNER TO postgres;

--
-- TOC entry 1654 (class 0 OID 0)
-- Dependencies: 1294
-- Name: locations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE locations_id_seq OWNED BY locations.id;


--
-- TOC entry 1290 (class 1259 OID 24608)
-- Dependencies: 4
-- Name: resources_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE resources_id_seq
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.resources_id_seq OWNER TO postgres;

--
-- TOC entry 1289 (class 1259 OID 24606)
-- Dependencies: 1636 1637 4
-- Name: resources; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE resources (
    id integer DEFAULT nextval('resources_id_seq'::regclass) NOT NULL,
    online boolean NOT NULL,
    serverid integer NOT NULL,
    url character varying(255) NOT NULL,
    norunningthreads integer DEFAULT 0 NOT NULL,
    supportedactions text NOT NULL
);


ALTER TABLE public.resources OWNER TO postgres;

--
-- TOC entry 1292 (class 1259 OID 24612)
-- Dependencies: 4
-- Name: servers_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE servers_id_seq
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.servers_id_seq OWNER TO postgres;

--
-- TOC entry 1291 (class 1259 OID 24610)
-- Dependencies: 1638 1639 1640 1641 1642 4
-- Name: servers; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE servers (
    id integer DEFAULT nextval('servers_id_seq'::regclass) NOT NULL,
    ipaddress character varying(15) NOT NULL,
    locationid integer NOT NULL,
    cpupower integer DEFAULT -1,
    memory integer DEFAULT -1,
    disksize integer DEFAULT -1,
    privateid integer DEFAULT 1 NOT NULL
);


ALTER TABLE public.servers OWNER TO postgres;

--
-- TOC entry 1299 (class 1259 OID 24711)
-- Dependencies: 4
-- Name: taskinputs; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE taskinputs (
    id integer NOT NULL,
    taskid character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    value text
);


ALTER TABLE public.taskinputs OWNER TO postgres;

--
-- TOC entry 1298 (class 1259 OID 24709)
-- Dependencies: 4 1299
-- Name: taskinputs_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE taskinputs_id_seq
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.taskinputs_id_seq OWNER TO postgres;

--
-- TOC entry 1655 (class 0 OID 0)
-- Dependencies: 1298
-- Name: taskinputs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE taskinputs_id_seq OWNED BY taskinputs.id;


--
-- TOC entry 1303 (class 1259 OID 24727)
-- Dependencies: 4
-- Name: taskmetaattributes; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE taskmetaattributes (
    id integer NOT NULL,
    taskid character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    value text
);


ALTER TABLE public.taskmetaattributes OWNER TO postgres;

--
-- TOC entry 1302 (class 1259 OID 24725)
-- Dependencies: 4 1303
-- Name: taskmetaattributes_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE taskmetaattributes_id_seq
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.taskmetaattributes_id_seq OWNER TO postgres;

--
-- TOC entry 1656 (class 0 OID 0)
-- Dependencies: 1302
-- Name: taskmetaattributes_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE taskmetaattributes_id_seq OWNED BY taskmetaattributes.id;


--
-- TOC entry 1301 (class 1259 OID 24719)
-- Dependencies: 4
-- Name: taskoutputs; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE taskoutputs (
    id integer NOT NULL,
    taskid character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    value text
);


ALTER TABLE public.taskoutputs OWNER TO postgres;

--
-- TOC entry 1300 (class 1259 OID 24717)
-- Dependencies: 1301 4
-- Name: taskoutputs_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE taskoutputs_id_seq
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.taskoutputs_id_seq OWNER TO postgres;

--
-- TOC entry 1657 (class 0 OID 0)
-- Dependencies: 1300
-- Name: taskoutputs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE taskoutputs_id_seq OWNED BY taskoutputs.id;


--
-- TOC entry 1295 (class 1259 OID 24618)
-- Dependencies: 4
-- Name: tasks; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE tasks (
    id integer NOT NULL,
    wfid character varying(255) NOT NULL,
    resourceid integer,
    statusid integer NOT NULL,
    creationdate date,
    creationtime time without time zone,
    lastupdatedate date,
    lastupdatetime time without time zone
);


ALTER TABLE public.tasks OWNER TO postgres;

--
-- TOC entry 1305 (class 1259 OID 24735)
-- Dependencies: 4
-- Name: taskuids; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE taskuids (
    id integer NOT NULL,
    uid character varying(255) NOT NULL,
    taskid integer NOT NULL
);


ALTER TABLE public.taskuids OWNER TO postgres;

--
-- TOC entry 1304 (class 1259 OID 24733)
-- Dependencies: 4 1305
-- Name: taskuids_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE taskuids_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.taskuids_id_seq OWNER TO postgres;

--
-- TOC entry 1658 (class 0 OID 0)
-- Dependencies: 1304
-- Name: taskuids_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE taskuids_id_seq OWNED BY taskuids.id;


--
-- TOC entry 1296 (class 1259 OID 24623)
-- Dependencies: 1644 4
-- Name: workflows; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE workflows (
    id character varying(255) NOT NULL,
    content text NOT NULL,
    creationdate date,
    creationtime time without time zone,
    parentid character varying(255),
    result text,
    status integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.workflows OWNER TO postgres;

--
-- TOC entry 1297 (class 1259 OID 24628)
-- Dependencies: 4
-- Name: workflows_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE workflows_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.workflows_id_seq OWNER TO postgres;

--
-- TOC entry 1643 (class 2604 OID 24640)
-- Dependencies: 1294 1293
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE locations ALTER COLUMN id SET DEFAULT nextval('locations_id_seq'::regclass);


--
-- TOC entry 1645 (class 2604 OID 24713)
-- Dependencies: 1299 1298 1299
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE taskinputs ALTER COLUMN id SET DEFAULT nextval('taskinputs_id_seq'::regclass);


--
-- TOC entry 1647 (class 2604 OID 24729)
-- Dependencies: 1303 1302 1303
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE taskmetaattributes ALTER COLUMN id SET DEFAULT nextval('taskmetaattributes_id_seq'::regclass);


--
-- TOC entry 1646 (class 2604 OID 24721)
-- Dependencies: 1301 1300 1301
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE taskoutputs ALTER COLUMN id SET DEFAULT nextval('taskoutputs_id_seq'::regclass);


--
-- TOC entry 1648 (class 2604 OID 24737)
-- Dependencies: 1305 1304 1305
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE taskuids ALTER COLUMN id SET DEFAULT nextval('taskuids_id_seq'::regclass);


--
-- TOC entry 1653 (class 0 OID 0)
-- Dependencies: 4
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


-- Completed on 2009-02-22 13:12:17

--
-- PostgreSQL database dump complete
--

