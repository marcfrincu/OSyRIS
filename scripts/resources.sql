-- The following data is given as an example. For it to work you need to adapt it to your settings.
-- For consistency you should use a tool such as psql (command line) or PgAdmin (graphical interface) for editing data

-- Add a new location
insert into locations (name) values ('Timisoara');

-- Add a new server. We set locationid=1 because we now that its a fresh database.
insert into servers (ipaddress, locationid, privateid) values ('127.0.0.1', 1, 1);

-- Add a new resource. We set the resource to be linked with serverid=1 and its URL=http://localhost:8080/axis/osyris/DummyService.jws. These also depend on your own settings.
insert into resources (online, serverid, supportedactions, url) values (true, 1, '', 'http://localhost:8080/axis/osyris/DummyService.jws');
