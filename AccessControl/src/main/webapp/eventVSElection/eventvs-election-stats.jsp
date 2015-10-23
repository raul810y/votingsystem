<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/vs-highcharts/highcharts-import.html" rel="import">

<dom-module name="eventvs-election-stats">
    <template>
        <style></style>
        <iron-ajax id="ajax" url="{{url}}" handle-as="json" last-response="{{statsDto}}" method="get" content-type="application/json"></iron-ajax>
        <div hidden="{{chartVisible}}" id="messageToUser" vertical layout center center-justified
             style="padding: 10px;font-weight: bold;">
            ${msg.withoutVotesLbl}
        </div>
        <div hidden="{{!chartVisible}}" id="chartDiv" vertical layout center center-justified
             style="margin:0 0 0 10px; height: 150px; min-width: 250px;"></div>
    </template>
    <script>
        Polymer({
            is:'eventvs-election-stats',
            properties: {
                eventvsId:{type:Number, observer:'loadStats'},
                statsDto:{type:Object, observer:'statsDtoChanged'}
            },
            options: {
                chart: {
                    plotBackgroundColor: null,
                    spacingTop: 10,
                    spacingBottom: 30,
                    spacingRight: 30,
                    plotBorderWidth: 0
                },
                title: {
                    text: this.caption
                },
                tooltip: {
                    pointFormat: '{series.name}: <b>{point.percentage:.1f}%</b>'
                },
                plotOptions: {
                    pie: {
                        allowPointSelect: true,
                        cursor: 'pointer',
                        dataLabels: {
                            enabled: false,
                            format: '<b>{point.name}</b>: {point.y} ${msg.votesLbl} - {point.percentage:.1f} %',
                            style: {
                                color: (Highcharts.theme && Highcharts.theme.contrastTextColor) || 'black'
                            }
                        }
                    }
                },
                series: [{
                    type: 'pie',
                    name: '${msg.votesLbl}',
                    data: [ ]
                }]
            },
            ready: function() {
                this.options.chart.renderTo = this.$.chartDiv
                this.chartVisible = false
                if(this.eventvsId != null) this.loadStats()
            },
            loadStats: function() {
                var targetURL = contextURL + "/rest/eventVSElection/id/" + this.eventvsId + "/stats"
                console.log(this.tagName + "- targetURL: " + targetURL)
                this.$.ajax.url = targetURL
                this.$.ajax.generateRequest()
            },
            statsDtoChanged: function() {
                if(this.statsDto == null) return
                var seriesData = []
                var numTotalVotes = 0
                Array.prototype.forEach.call(this.statsDto.fieldsEventVS, function(fieldEvent) {
                    seriesData.push([fieldEvent.content, fieldEvent.numVotesVS])
                    numTotalVotes += fieldEvent.numVotesVS
                });
                this.options.series[0].data = seriesData
                console.log(this.tagName + " - subject: " + this.statsDto.subject + " - numTotalVotes: " + numTotalVotes);
                if(numTotalVotes > 0) {
                    var chart = new Highcharts.Chart(this.options);
                    this.chartVisible = true
                }
            }
        });
    </script>
</dom-module>