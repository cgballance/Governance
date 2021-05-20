# Governance

For commercial organizations, the legal department worries about the risk of using software that is not owned by the contributor.  Additionally,
the software could be of a license that doesn't allow them to have free usage of the software.  From an architect's view, the software should be
well used in the field and have active contributions.  Software already exists to ensure that the licensing and activity is adaquate.  Black Duck Software,
JFrog and others sell products for this space.  That's all great.  However, what if you want even greater control of the lifecycle of artifact usage.
What if you want to be able to manage your technical debt.  These products don't go deep enough.  In this light, I decided that by interjecting a system
into the build, we can provide the ability to do a better job at this.

## Toolchains
Maven and Gradle are supported by the initial version of this software.  I intended to support npm as well, but the tool doesn't have the right hooks to do a good job
at managing nodejs artifacts.  Artifacts in npm do not distinguish hierarchical dependencies, the namespace is flat.  For now, I'm sticking with the Java space.
The goal is not to manage EVERY artifact that is used by project, but to manage directly used libraries, which may depend on many subordinate ones.

## Requirements
The set of tools that i created rely on developers marking their build tool specific configuration files(pom.xml,build.gradle) with project information.
The project identifier is named 'acronym', in the software.  It is the key used for identifying the 'who' in the toolchain.  The 'what' is the artifacts used.
The 'acronym' is the permissioned subject.  The same acronym may be used for multiple builds, all of which will be permissioned that same exact way.  This is the right
granularity for managing the subject namespace.
The other requirement is that the build system should have the ability to re-write the configuration files(pom.xml,build.gradle) prior to performing the build.  There are
a couple reasons for this.  First, there is database security information that has to show up somewhere for the integration point to be able to read/write into a relational
database.  Second, we need to enforce that the project correctly uses(does not bypass) this software.  Finally, we want to provide isolation so that you can decide not to
use this sofware or apply changes(versioning) efficiently.

## Inventory
### API Services
### Administration Application
### Build plugins
#### Maven
#### Gradle
### Test project
