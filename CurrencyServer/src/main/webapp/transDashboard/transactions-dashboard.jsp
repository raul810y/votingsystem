<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/vs-checkbox-selector/vs-checkbox-selector.html" rel="import"/>
<link href="../resources/bower_components/vs-datepicker/vs-datepicker.html" rel="import"/>

<link href="../resources/d3.html" rel="import"/>
<link href="transactions-treemap.vsp" rel="import"/>
<link href="transactions-scatter.vsp" rel="import"/>

<dom-module name="transactions-dashboard">
    <style>
        :host {
            display: block;
            width:100%;
            height: 100%;
            position: relative;
        }
    </style>
    <template>
        <div id="fullScreenDiv" style="display: none;"></div>
        <div class="horizontal layout center center-justified">
            <div id="selectorDiv" class="vertical layout center" style="border: 1px solid #ccc; margin: 5px;border-radius: 3px;">
                <div style="color: #888;font-weight: bold;text-decoration: underline;">${msg.filtersLbl}</div>
                <div class="horizontal layout" style="font-size: 0.9em;margin: 5px 0 5px 0;">
                    <vs-datepicker id="dateFromDatepicker" years-back="5" month-labels='[${msg.monthsShort}]' day-labels='[${msg.weekdaysShort}]' time-enabled
                        caption="${msg.fromLbl}" style="margin:0 5px 0 0;"> </vs-datepicker>
                    <vs-datepicker id="dateToDatepicker" years-back="5" month-labels='[${msg.monthsShort}]' day-labels='[${msg.weekdaysShort}]' time-enabled
                                   caption="${msg.toLbl}"> </vs-datepicker>
                </div>
                <div class="horizontal layout center center-justified" style="width: 260px; margin: 0 5px 0 5px; font-size: 0.75em; padding: 0 6px 0 6px;">
                    <input id="timeLimitedCheckBox" type="checkbox" value="timeLimited" on-click="timeCheckboxSelected" checked="true" style="">
                    <span style="color:#ba0011;"><i class="fa fa-clock-o"></i> ${msg.timeLimitedLbl}</span>
                    <input id="timeFreeCheckBox" type="checkbox" value="timeFree" on-click="timeCheckboxSelected" style="margin:0 0 0 10px;" checked="true">${msg.timeFreeLbl}
                </div>
                <div style="margin: 5px 10px 0 10px;">
                    <vs-checkbox-selector id="currencySelector" width="250px" caption="${msg.currencyLbl}" on-checkbox-selected="currencyCheckboxSelected"></vs-checkbox-selector>
                </div>
                <div style="margin: 5px 10px 0 10px;">
                    <vs-checkbox-selector id="typeSelector" width="250px" caption="${msg.movementTypeLbl}" on-checkbox-selected="typeCheckboxSelected"></vs-checkbox-selector>
                </div>
                <div style="margin: 5px 10px 5px 10px;">
                    <vs-checkbox-selector id="tagSelector" width="250px" caption="${msg.tagLbl}" on-checkbox-selected="tagCheckboxSelected"></vs-checkbox-selector>
                </div>
                <div class="horizontal layout"  style="margin: 5px 10px 5px 10px;">
                    <input type="text" id="searchInput" value='' style="width: 140px;margin:0 10px 0 0;" placeholder="${msg.searchLbl}"/>
                    <button on-click="getHTTP"><i id="searchIcon" class="fa fa-search" style="margin:0px 3px 0px 0px;" ></i> ${msg.searchLbl}</button>
                </div>
            </div>
            <div id="treemapZoomableDiv">
                <transactions-treemap id="transactionTreemap" on-filter-request="filterChart"></transactions-treemap>
            </div>
        </div>
        <div>
            <transactions-scatter id="transactionsScatter" style="height: 300px; width: 100%;"></transactions-scatter>
        </div>
    </template>
    <script>
    (function() {
        Polymer({
            is:'transactions-dashboard',
            properties: {
                orderBy: {type: String, value:"orderByType"}
            },
            ready: function() {
                //this.$.dateFromDatepicker.setDate(new Date().getMonday())
                this.$.dateFromDatepicker.setDate(DateUtils.parseInputType("2015-11-01"))
                this.$.dateToDatepicker.setDate(new Date())

                this.formatDate = d3.time.format("%Y%m%d_%M%S");

                this.getHTTP()
            },
            timeCheckboxSelected:function (e) {
                console.log("timeCheckboxSelected: " + e.target.value + " - checked: " + e.target.checked)
                if(e.target.checked) {
                    var index = this.transStats.transactionTimeFilter.indexOf(e.target.value)
                    this.transStats.transactionTimeFilter.splice(index, 1)
                } else {
                    this.transStats.transactionTimeFilter.push(e.target.value)
                }
                this.filterChart()
            },
            currencyCheckboxSelected: function(e) {
                if(e.detail.checked) {
                    var index = this.transStats.transactionCurrencyFilter.indexOf(e.detail.value)
                    this.transStats.transactionCurrencyFilter.splice(index, 1)
                } else {
                    this.transStats.transactionCurrencyFilter.push(e.detail.value)
                }
                this.filterChart()
            },
            typeCheckboxSelected: function(e) {
                if(e.detail.checked) {
                    var index = this.transStats.transactionTypeFilter.indexOf(e.detail.value)
                    this.transStats.transactionTypeFilter.splice(index, 1)
                } else {
                    this.transStats.transactionTypeFilter.push(e.detail.value)
                }
                this.filterChart()
            },
            tagCheckboxSelected: function(e) {
                if(e.detail.checked) {
                    var index = this.transStats.transactionTagFilter.indexOf(e.detail.value)
                    this.transStats.transactionTagFilter.splice(index, 1)
                } else {
                    this.transStats.transactionTagFilter.push(e.detail.value)
                }
                this.filterChart()
            },
            filterChart:function (e) {
                this.$.transactionsScatter.filterChart(this.transStats)
                var filteredTransStats = new TransactionsStats()
                if(e && e.detail) this.orderBy = e.detail
                this.chartData.forEach(function(transactionvs) {
                    if(!this.transStats.checkFilters(transactionvs).filtered) filteredTransStats.pushTransaction(transactionvs, this.orderBy)
                }.bind(this))
                if(this.orderBy === "orderByType") treemapData = filteredTransStats.transactionsTreeByType
                if(this.orderBy === "orderByTag") treemapData = filteredTransStats.transactionsTreeByTag
                TransactionsStats.setCurrencyPercentages(treemapData)
                this.$.transactionTreemap.chart(treemapData)
            },
            getHTTP:function() {
                var targetURL = "/CurrencyServer/rest/transactionVS/from/" + this.formatDate(this.$.dateFromDatepicker.getDate()) +
                                "/to/" + this.formatDate(this.$.dateToDatepicker.getDate())
                if(this.$.searchInput.value != null && this.$.searchInput.value.trim() !== "") {
                    targetURL = targetURL + "?searchText=" + this.$.searchInput.value.trim()
                }
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                d3.json(targetURL, function (json) {
                    this.chartData = json.resultList
                    this.transStats = new TransactionsStats()
                    this.chartData.forEach(function(transactionvs) {
                        this.transStats.pushTransaction(transactionvs, "orderByType")
                    }.bind(this))
                    this.$.tagSelector.init(this.transStats.tags, function (item) {
                        return 'font-weight: bold; color: ' + TransactionsStats.getColorScale(item) + ';' }.bind(this), null)
                    this.$.typeSelector.init(this.transStats.transactionTypes, function (item) {
                            return 'font-weight: bold; color: ' + TransactionsStats.getColorScale(item) + ';'
                        }.bind(this), function (transaction) {
                            return transactionsMap[transaction].lbl
                        })
                    this.$.currencySelector.init(this.transStats.currencyCodes, function (item) {
                        return 'font-weight: bold; color: ' + TransactionsStats.getColorScale(item) + ';' }.bind(this), null)

                    TransactionsStats.setCurrencyPercentages(this.transStats.transactionsTreeByType)
                    this.$.transactionsScatter.chart(this.chartData)
                    this.$.transactionTreemap.chart(this.transStats.transactionsTreeByType)
                }.bind(this))
            }
        });
    })();
    </script>
</dom-module>