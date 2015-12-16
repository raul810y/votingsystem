<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/d3.html" rel="import"/>

<dom-module id="treemap-zoomable">
    <style>
        :host {
            display: block;
            width: 100%;
            height: 100%;
            position: relative;
        }

        #chart {
            background: #ddd;
        }

        text {
            pointer-events: none;
        }

        .breadcrumb {
            cursor: pointer;
            pointer-events: all;
        }

        .breadcrumb text {
            font-weight: bold;
            fill: #fff;
            font-size: 0.9em;
        }

        .breadcrumb .breadcrumbAmount{
            font-size: 0.7em;
        }
        .childText {
            font-style: italic;
            fill: #555;
        }

        .parentText {
            fill: #fff;
            font-size: 0.9em;
            font-weight: bold;
        }

        rect {
            fill: none;
            stroke: #fff;
        }

        rect.parent,
        .header rect {
            stroke-width: 2px;
        }

        .header rect {
            fill: orange;
        }

        .header:hover rect {
            fill: #ee9700;
        }

        .children rect.parent,
        .header rect {
            cursor: pointer;
        }

        .children rect.parent {
            fill: #bbb;
            fill-opacity: .5;
        }

        .children:hover rect.child {
            fill-opacity: .5;
        }

        .tooltip {
            text-align: center;
            font-size: 0.8em;
            position: absolute;
            color: #888;
            padding: 3px;
            background: #fafafa;
            border: 1px solid #888;
            border-radius: 8px;
            pointer-events: none;
        }
        .tooltip .header1{
            font-weight: bold;
        }
        .tooltip .header2{
            margin: 0 5px 0 5px;
            text-decoration: underline;
            font-style: italic;
            font-size: 0.9em;
        }
        .tooltip .content{
            font-size: 0.8em;
            margin: 0 5px 0 5px;
            min-height: 1.1em;
        }
        .timeLimitedDiv {
            color:#ba0011;
        }
    </style>
    <template>
        <div id="messageDiv" class="horizontal layout center center-justified flex" style="margin:20px;text-align: center;">${msg.withoutDataLbl}</div>
        <div id="chartContainerDiv" style="display: none;">
            <div id="sequenceMessageDiv" style="text-align: center; font-weight: bold; color: #888; font-size: 1.6em; font-style: italic;">{{sequenceMessage}}</div>
            <div id="sequence"></div>
            <div id="chart" style="padding: 0px; margin: 0px;"></div>
            <div id="tooltip" class="tooltip" style="display: none;">
                <div style$="{{_getSelectedNodeTooltipHeaderStyle(selectedNode)}}">{{_getDescriptionWithPercentage(selectedNode)}}</div>
                <div class="horizontal layout center center-justified">
                    <div>
                        <div class="header1">${msg.totalLbl}</div>
                        <div class="horizontal layout center center-justified">
                            <div>
                                <div class="header2">${msg.movementsLbl}</div>
                                <div class="content">{{selectedNode.numTotalTransactions}}</div>
                            </div>
                            <div>
                                <div class="header2">${msg.amountLbl}</div>
                                <div class="content">{{formatMoney(selectedNode.totalAmount)}} {{selectedCurrencyCode}}</div>
                            </div>
                        </div>
                    </div>
                    <div style="margin: 0 0 0 10px;" class="timeLimitedDiv">
                        <div class="header1" style="color:#ba0011;"><i class="fa fa-clock-o"></i> ${msg.timeLimitedLbl}</div>
                        <div class="horizontal layout center center-jsutified">
                            <div>
                                <div class="header2" style="color:#ba0011;">${msg.movementsLbl}</div>
                                <div class="content">{{selectedNode.numTimeLimitedTransactions}}</div>
                            </div>
                            <div>
                                <div class="header2" style="color:#ba0011;">${msg.amountLbl}</div>
                                <div class="content">{{formatMoney(selectedNode.timeLimitedAmount)}} {{selectedCurrencyCode}} {{selectedNodeTimeLimitedPercentage}}</div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </template>
    <script>
        (function () {
            Polymer({
                is: "treemap-zoomable",
                properties: {
                    selectedNode: {type: Object, observer: 'selectedNodeChanged'},
                    breadcrumb: {type: Object, value: {width: 205, height: 30, s: 3, t: 10}},
                    margin: {type: Object, value:  {top: 0, right: 0, bottom: 0, left: 0}},
                    selectedCurrencyCode: {type: String},
                    width: {type: Number, value:500},
                    height: {type: Number, value:300}
                },
                ready: function () {
                    this.$.chart.style.height = this.height + this.margin.top + this.margin.bottom + "px";
                    this.$.sequenceMessageDiv.style.height = this.breadcrumb.height + "px";

                    this.height = this.height - this.margin.top - this.margin.bottom
                    this.x = d3.scale.linear().domain([0, this.width]).range([0, this.width]);
                    this.y = d3.scale.linear().domain([0, this.height]).range([0, this.height]);

                    this.treemap = d3.layout.treemap()
                            .value(function(d) {return d.totalAmount})
                            .children(function (d, depth) {
                                return depth ? null : d._children;
                            })
                            .sort(function (a, b) {
                                return a.value - b.value;
                            })
                            .ratio(this.height / this.width * 0.5 * (1 + Math.sqrt(5)))
                            .round(false);

                    var trail = d3.select(this).select("#sequence").append("svg")
                            .attr("width", this.width)
                            .attr("height", this.breadcrumb.height)
                            .attr("id", "trail");

                },
                filterChart:function (transStats) {

                },
                getAncestors: function (node) {
                    var path = [];
                    var current = node;
                    while (current.parent) {
                        path.unshift(current);
                        current = current.parent;
                    }
                    return path;
                },
                _getDescriptionWithPercentage: function (node) {
                    var percentage = node.percentage
                    if (!percentage && node.parent) {
                        percentage = TransactionsStats.getPercentage(node.value, node.parent.value)
                    }
                    if(percentage) return (node.description || node.name) + " " + percentage + "%";
                },
                _getSelectedNodeTooltipHeaderStyle: function (node) {
                    return "font-weight: bold; font-size: 1.1em;color:" + this.color(node.name) + ";"
                },
                init: function (color) {
                    this.color = color
                    window.addEventListener('resize', function (event) {
                        //this.circlesGradientList = []
                        //this.chart(this.chartData)
                    }.bind(this));
                },
                selectedNodeChanged: function () {
                    var node = this.selectedNode
                    while (!node.currencyCode) node = node.parent
                    this.selectedCurrencyCode = node.currencyCode
                    this.selectedNodeTimeLimitedPercentage = "(" + TransactionsStats.getPercentage(
                            this.selectedNode.timeLimitedAmount, this.selectedNode.totalAmount) + " %)"
                },
                formatMoney: function (amount) {
                    return amount.formatMoney()
                },
                chart: function (root, color) {
                    console.log(this.tagName + " - chart - " + JSON.stringify(root))
                    if(!root || !root.children || root.children.length === 0) {
                        console.log(this.tagName + " - without root data")
                        this.$.messageDiv.style.display = 'block'
                        this.$.chartContainerDiv.style.display = 'none'
                        return
                    } else this.$.messageDiv.style.display = 'none'


                    var currenciesTotalAmount = 0
                    root.children.forEach(function(node) {
                        currenciesTotalAmount += node.totalAmount
                    })
                    this.sequenceMessage = currenciesTotalAmount.formatMoney() + " " + root.exchangeCurrency

                    if(this.$.chart.childNodes[0]) this.$.chart.removeChild(this.$.chart.childNodes[0])
                    this.svg = d3.select(this).select("#chart").append("svg")
                            .attr("width", this.width + this.margin.left + this.margin.right)
                            .attr("height", this.height + this.margin.bottom + this.margin.top)
                            .style("margin-left", -this.margin.left + "px")
                            .style("margin.right", -this.margin.right + "px")
                            .append("g")
                            .attr("transform", "translate(" + this.margin.left + "," + this.margin.top + ")")
                            .style("shape-rendering", "crispEdges");
                    this.$.chartContainerDiv.style.display = 'block'

                    var hostElement = this;
                    var transitioning
                    this.color = color

                    initialize(root);
                    accumulate(root);
                    layout(root);
                    display(root);

                    function initialize(root) {
                        root.x = root.y = 0;
                        root.dx = hostElement.width;
                        root.dy = hostElement.height;
                        root.depth = 0;
                    }

                    // Aggregate the values for internal nodes. This is normally done by the
                    // treemap layout, but not here because of our custom implementation.
                    // We also take a snapshot of the original children (_children) to avoid
                    // the children being overwritten when when layout is computed.
                    function accumulate(d) {
                        if(d.children && d.children.length > 0) {
                            d._children = d.children
                            d.children.forEach(function (d) { accumulate(d)})
                        }
                    }

                    // Compute the treemap layout recursively such that each group of siblings
                    // uses the same size (1×1) rather than the dimensions of the parent cell.
                    // This optimizes the layout for the current zoom state. Note that a wrapper
                    // object is created for the parent node for each group of siblings so that
                    // the parent’s dimensions are not discarded as we recurse. Since each group
                    // of sibling was laid out in 1×1, we must rescale to fit using absolute
                    // coordinates. This lets us use a viewport to zoom.
                    function layout(d) {
                        if (d._children) {
                            hostElement.treemap.nodes({_children: d._children});
                            d._children.forEach(function (c) {
                                c.x = d.x + c.x * d.dx;
                                c.y = d.y + c.y * d.dy;
                                c.dx *= d.dx;
                                c.dy *= d.dy;
                                c.parent = d;
                                layout(c);
                            });
                        }
                    }

                    function display(d) {
                        console.log("display - name: " + d.name + " - d.depth: " + d.depth)
                        if(!d.parent) {
                            hostElement.$.sequenceMessageDiv.style.display = ''
                            hostElement.$.sequence.style.display = 'none'
                        } else {
                            hostElement.$.sequenceMessageDiv.style.display = 'none'
                            hostElement.$.sequence.style.display = 'block'
                        }

                        var breadcumGroup = d3.select(this).select("#trail").selectAll("g")
                                .data(hostElement.getAncestors(d));
                        breadcumGroup.enter().append("g").attr("class", "breadcrumb");
                        breadcumGroup.append("polygon")
                                .attr("points", breadcrumbPoints)
                                .on("click", function (d) {
                                    transition(d.parent, -1)
                                })
                                .style("fill", function (d) {
                                    return hostElement.color(d.name);
                                });
                        breadcumGroup.append("text")
                                .attr("x", (hostElement.breadcrumb.width + hostElement.breadcrumb.t) / 2)
                                .attr("y", hostElement.breadcrumb.height / 2)
                                .attr("dy", "-0.10em")
                                .attr("text-anchor", "middle")
                                .text(function (d) { return  hostElement._getDescriptionWithPercentage(d) });
                        breadcumGroup.append("text")
                                .attr("class", "breadcrumbAmount")
                                .attr("x", (hostElement.breadcrumb.width + hostElement.breadcrumb.t) / 2)
                                .attr("y", hostElement.breadcrumb.height / 2)
                                .attr("dy", "1em")
                                .attr("text-anchor", "middle")
                                .text(function (d) { return d.totalAmount.formatMoney() + " " + hostElement.selectedCurrencyCode });


                        // Set position for entering and updating nodes.
                        breadcumGroup.attr("transform", function (d, i) {
                            return "translate(" + i * (hostElement.breadcrumb.width + hostElement.breadcrumb.s) + ", 0)";
                        });
                        breadcumGroup.exit().remove();
                        Polymer.dom(hostElement.$.sequence).appendChild(Polymer.dom(hostElement.$.sequence).childNodes[0])

                        var g1 = hostElement.svg.insert("g", ".header")
                                .datum(d)
                                .attr("class", "depth");

                        var g = g1.selectAll("g")
                                .data(d._children)
                                .enter().append("g");

                        g.filter(function (d) {
                                    return d._children;
                                })
                                .classed("children", true)
                                .on("click", function (d) {
                                    transition(d, 1)
                                });

                        var childNodes = g.selectAll(".child")
                                .data(function (d) {
                                    return d._children || [d];
                                })
                                .enter()
                                .append("rect")
                                .attr("class", "child")
                                .call(rect)

                        g.append("rect")
                                .attr("class", "parent")
                                .call(rect)

                        g.selectAll("rect")
                                .on("mouseout", handleMouseOut)
                                .on("mouseover", handleMouseOver)

                        g.append("text")
                                .attr("dy", ".75em")
                                .attr("class", "parentText")
                                .text(function (d) {
                                    return hostElement._getDescriptionWithPercentage(d);
                                })
                                .call(text);

                        var childTextList = g.selectAll(".childText")
                                .data(function (d) {
                                    return d._children || []
                                })
                        childTextList.enter().append("text")
                                .attr("class", "childText")
                                .style({'font-size': '0.6em'})
                                .text(function (d) {
                                    return d.description || d.name
                                })
                        childTextList.call(childText)

                        function transition(d, increment) {
                            handleMouseOut()
                            if (transitioning || !d) return;
                            transitioning = true;

                            var g2 = display(d),
                                    t1 = g1.transition().duration(750),
                                    t2 = g2.transition().duration(750);

                            // Update the domain only after entering new elements.
                            hostElement.x.domain([d.x, d.x + d.dx]);
                            hostElement.y.domain([d.y, d.y + d.dy]);

                            // Enable anti-aliasing during the transition.
                            hostElement.svg.style("shape-rendering", null);
                            // Draw child nodes on top of parent nodes.
                            hostElement.svg.selectAll(".depth").sort(function (a, b) {
                                return a.depth - b.depth;
                            });
                            // Fade-in entering text.
                            g2.selectAll("text").style("fill-opacity", 0);
                            // Transition to the new view.
                            t1.selectAll("text").call(text).style("fill-opacity", 0);
                            t2.selectAll("text").call(text).style("fill-opacity", 1);
                            t1.selectAll("rect").call(rect);
                            t2.selectAll("rect").call(rect);
                            t1.selectAll(".childText").call(childText).style("fill-opacity", 0);
                            t2.selectAll(".childText").call(childText).style("fill-opacity", 1);

                            // Remove the old node when the transition is finished.
                            t1.remove().each("end", function () {
                                hostElement.svg.style("shape-rendering", "crispEdges");
                                transitioning = false;
                            });
                            fillRects()
                        }

                        function breadcrumbPoints(d, i) {
                            // Generate a string that describes the points of a breadcrumb polygon.
                            var points = [];
                            points.push("0,0");
                            points.push(hostElement.breadcrumb.width + ",0");
                            points.push(hostElement.breadcrumb.width + hostElement.breadcrumb.t + "," + (hostElement.breadcrumb.height / 2));
                            points.push(hostElement.breadcrumb.width + "," + hostElement.breadcrumb.height);
                            points.push("0," + hostElement.breadcrumb.height);
                            if (i > 0) { // Leftmost breadcrumb; don't include 6th vertex.
                                points.push(hostElement.breadcrumb.t + "," + (hostElement.breadcrumb.height / 2));
                            }
                            return points.join(" ");
                        }

                        fillRects()
                        Polymer.dom(hostElement.$.chart).appendChild(Polymer.dom(hostElement.$.chart).childNodes[0])
                        return g;
                    }

                    function fillRects() {
                        hostElement.svg.selectAll("rect.parent")
                                .style("fill", function (d) {
                                    if (d.name) {
                                        return color(d.name)
                                    }
                                }.bind(this))
                    }

                    function text(text) {
                        text.attr("x", function (d) {
                                    return hostElement.x(d.x) + 6;
                                })
                                .attr("y", function (d) {
                                    return hostElement.y(d.y) + 6;
                                });
                    }

                    function breadcumText(text) {
                        header
                    }
                    function childText(text) {
                        text.attr("x", function (d) {
                                    var bbox = d3.select(this).node().getBBox();
                                    return hostElement.x(d.x) + hostElement.x(d.dx) - bbox.width - 5
                                })
                                .attr("y", function (d) {
                                    var bbox = d3.select(this).node().getBBox();
                                    return hostElement.y(d.y) + hostElement.y(d.dy) - 2
                                });
                    }

                    function rect(rect) {
                        rect.attr("x", function (d) {
                                    return hostElement.x(d.x);
                                })
                                .attr("y", function (d) {
                                    return hostElement.y(d.y);
                                })
                                .attr("width", function (d) {
                                    return hostElement.x(d.x + d.dx) - hostElement.x(d.x);
                                })
                                .attr("height", function (d) {
                                    return hostElement.y(d.y + d.dy) - hostElement.y(d.y);
                                })
                    }

                    function handleMouseOut(d, i) {
                        hostElement.$.tooltip.style.display = 'none'
                    }

                    function handleMouseOver(d, i) {
                        var hostElementOffsets = hostElement.$.chart.getBoundingClientRect();
                        hostElement.selectedNode = d
                        hostElement.$.tooltip.style.display = 'block'
                        var tooltipOffsets = hostElement.$.tooltip.getBoundingClientRect();
                        hostElement.$.tooltip.style.top = hostElementOffsets.top + "px"
                        hostElement.$.tooltip.style.left = (hostElementOffsets.width / 2 - tooltipOffsets.width/2)  + "px"
                    }

                }
            });
        })();
    </script>
</dom-module>