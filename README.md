# OSyRIS

# Description

The OSyRIS engine is a rule based workflow engine inspired from the chemical paradigm. It was part of my Ph.D. research and was incorporated in several EU (mOSAIC) and ESA (GiSHEO) projects.

It also supports a distributed version called D-OSyRIS which allows rules to run in parallel similar to a chemical reaction.

The engine is extensible by adding customized functionality.

It relies on Drools to execute SiLK (a language I invented) scripts. 

# Published papers

- M. Frincu, [D-OSyRIS: A Self-Healing Distributed Workflow Engine](https://ieeexplore.ieee.org/document/6108276), Procs. ISPDC 2011.
- M Frincu, D. Petcu, [OSyRIS: a Nature Inspired Workflow Engine for Service Oriented Environments](https://www.scpe.org/index.php/scpe/article/view/642), SCPE, vol. 11(1), 2010.

# Usage

The repository contains the following folders and files:

- **lib**: all the required jars.
- **rules**: examples of drl rules automatically created by the engine.
- **scripts**: some scripts to create the PostgreSQL database.
- **silk**: example of rule based workflows in SiLK. It includes the silk files corresponding to the two examples in the *rules* folder. It also contains some examples for image processing.
- **src**: contains all the necessary source code of the engine. In **src/osyris/samples/tests** you can see some examples.

# License

GPL 3.0
