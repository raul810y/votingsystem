<% def jarArchives = "VotingToolApplet.jar"
	String depsPath = "${grailsApplication.mainContext.getResource('.')?.getFile()}/applet/lib"
	def appletJarDependencies = []
	new File(depsPath).eachFile() { file->
		if(file.path.endsWith(".jar"))
			appletJarDependencies.add(file.getName())
	}
	appletJarDependencies?.each {
		jarArchives = jarArchives.concat(",lib/${it}")
	}
	
%>
<!DOCTYPE html>
<html>
    <head>
        <r:layoutResources />
    </head>
    <body>
		<APPLET CODEBASE="${grailsApplication.config.grails.serverURL}/applet"
     		CODE="org.votingsystem.applet.votingtool.VotingApplet"
     		ARCHIVE="${jarArchives}"
     		HEIGHT=200
     		WIDTH=600>
            <param name="permissions" value="all-permissions" />
		</APPLET>
    </body>
</html>
<r:script>

		function setMessageFromSignatureClient(message) {
			return parent.votingSystemClient.setMessageFromSignatureClient(message)
		}

		function getMessageToSignatureClient() {
			return parent.votingSystemClient.getMessageToSignatureClient()
		}

</r:script>
<r:layoutResources />
