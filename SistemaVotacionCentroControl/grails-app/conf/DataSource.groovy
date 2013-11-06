/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
dataSource {
	pooled = true
	driverClassName = "org.postgresql.Driver"
	dialect = org.hibernate.dialect.PostgreSQLDialect
	username = "usuariovotacion"
	password = "usuariovotacion"
}

hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = false
//    cache.region.factory_class = 'org.hibernate.cache.ehcache.EhCacheRegionFactory' // Hibernate 4
    cache.region.factory_class = 'net.sf.ehcache.hibernate.EhCacheRegionFactory'
	hibernate.search.default.indexBase = new File("./VotingSystem/searchIndexCentroControl").absolutePath
	//hibernate.show_sql=true
}


// environment specific settings
environments {
    development {
        dataSource {
            dbCreate = "update" // one of 'create', 'create-drop','update'
			url="jdbc:postgresql://localhost:5432/SistemaVotacionCentroControl"
        }
    }
    test {
        dataSource {
            dbCreate = "create-drop"
			url="jdbc:postgresql://localhost:5432/SistemaVotacionCentroControlTest"
        }
    }
    /*production {
		dataSource {
			pooled = true
			dbCreate = "update"
			url="jdbc:postgresql://localhost:5432/SistemaVotacionCentroControl"
			properties {
				maxActive = 100
				maxIdle = 30
				minIdle = 5
				initialSize = 5
				minEvictableIdleTimeMillis = 60000
				timeBetweenEvictionRunsMillis = 60000
				maxWait = 10000
				validationQuery = "/* ping *//*"
			}
		}
    }*/
	
	production {
		dataSource {
			pooled = true
			dbCreate = "update"
			jndiName = "java:comp/env/jdbc/centrocontrol"
		}
	}
	/*production {
		dataSource {
			dbCreate = "update" // one of 'create', 'create-drop','update'
			url="jdbc:postgresql://localhost:5432/SistemaVotacionCentroControl"
		}
	}*/
}
