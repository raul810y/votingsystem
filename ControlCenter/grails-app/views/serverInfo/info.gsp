<html>
    <head>
   		<r:require modules="application"/>
        <style type="text/css" media="screen">
        	#content a{margin: 0px 100px 0px 0px;}
        </style>        
        <r:layoutResources/>
    </head>
    <body>
		<div class="container">
        	<a class="headerTitle" href="${grailsApplication.config.grails.serverURL}">${message(code: 'serverNameLabel', null)}</a>
			<div id="tabs" style="min-height: 700px;">
			  <ul>
			    <li><a href="#tabs-1" style="font-size: 0.8em;"><span><g:message code="infoLabel"/></span></a></li>
			    <li><a href="serviceList" style="font-size: 0.8em;"><span><g:message code="serviceURLSMsg"/></span></a></li>
			    <li><a href="appData" style="font-size: 0.8em;"><span><g:message code="appDataLabel"/></span></a></li>
			  </ul>
			  <div id="tabs-1">
			  	<div class="container"  style="height:100%;">
			  		<div id="content" class="content">
			           <div class="mainLinkContainer">
				           <div class="mainLink"><a href="${grailsApplication.config.grails.serverURL}/">${message(code: 'mainPageLabel', null)}</a></div>
				           <div class="mainLink"><a href="https://github.com/jgzornoza/SistemaVotacion/tree/master/AccessControl">${message(code: 'sourceCodeLabel', null)}</a></div>
				           <div class="mainLink"><a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Control-de-Acceso">${message(code: 'wikiLabel', null)}</a></div>
			           </div>
			           <p id="contentText" style="margin: 40px 0px 0px 0px;">${message(code: 'urlMatch', null)}: <b>${grailsApplication.config.grails.serverURL}</b></p>
   			           <p>
                           <i class="fa fa-gears" style="color:#388746;"></i>
                           <a id="clientToolLink" class="appLink" style="color: #09287e; font-size: 0.9em;"
                              href="${grailsApplication.config.grails.serverURL}/app/ClientTool.zip">
                               <g:message code="clientToolLinkText"/>
                           </a>
			           </p>
					</div>
			  	</div>
			  </div>
			</div>
			<div class="infoFooter" style="margin: 0px auto 20px 0;width:100%;font-size: 0.7em; position:relative;">
				<p style="text-align: center;">
					<a  class="appLink" href="mailto:${grailsApplication.config.VotingSystem.emailAdmin}">
						${message(code: 'emailLabel', null)}</a>
				</p>	
		   	</div>	
		<div>
	</body>
</html>
<r:script>
    $(function() {
        $( "#tabs" ).tabs()
    });
</r:script>
<r:layoutResources/>