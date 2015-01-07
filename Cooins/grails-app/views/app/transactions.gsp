<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
</head>
<body>
    <div class="pageContentDiv">
        <h1 class="pageHeader" style="text-align: center;">Transacciones</h1>
        <div class="text-justify" style="margin: 0 auto 0 auto; font-size: 1.2em;">
            <p>El intercambio monetario puede producirse mediante <b>transacciones firmadas</b>,
            <b>transacciones firmadas anónimas</b>, o mediante intercambio de directo de <b>cooins</b></p>
            <ul><li>En las <b>transacciones firmadas</b> queda perfectamente definida la identidad del pagador y del
            cobrador.</li>
            <li>En las <b>transacciones firmadas anónima</b> queda perfectamente definida sólo la identidad del cobrador,
            la identidad del pagador es anónima. Para este tipo de transacciones es necesario utilizar un <b>cooin</b>.</li>
            <li>En los <b>intercambios directos de cooins</b> permanecen anónimos tanto el cobrador como el pagador (aunque
            existen mecanismos para poder evitar fraudes ...).</li></ul>

            <h4>Notas sobre transacciones firmadas</h4>
            <ul>
                <li>Los usuarios no tienen asociada ninguna etiqueta, cuando un usuario hace una transacción directa
                a otro usuario el dinero sale de la cuenta de <b>gastos libres</b></li>
                <li>Es posible hacer un <b>ingreso con etiqueta</b> a otro usuario pero el intercambio directo entre usuarios procede siempre de la cuenta de
                <b>gastos libres</b>, es decir, aunque un usuario tenga disponible en su cuenta asociada a esa
                etiqueta, si el ingreso es a otro <b>usuario</b> el dinero se extraerá de la cuenta de <b>gastos libres</b></li>
            </ul>
            <h4>Notas sobre cooins</h4>
            <ul>
                <li>El dinero para la retirada de cooins sale siempre de la cuenta de <b>gastos libres</b></li>
                <li>El valor de un <b>cooin</b> se puede hacer llegar a otro usuario utilizando el <b>cooin</b> en una
                <b>transacción firmada anónima</b>, en cuyo caso el importe se ingresa en la cuenta del usuario,
                o entregando diréctamente el <b>cooin</b></li>
                <li>Un <b>cooin</b> pueden tener asociado una etiqueta para que la <b>transacción anónima firmada</b>
                sólo pueda producirse a servicios o productos asociados a esa etiqueta</li>
            </ul>

            <h3 class="pageHeader">Preguntas frecuentes</h3>
            <p><b>¿Qué es que un 'ingreso con etiqueta'?</b> Un igreso en el que el receptor sólo puede emplear el
            importe en productos o servicios asociados a esa etiqueta.
            </p>
            <p><b>¿Qué es la etiqueta 'WILDTAG'?</b> Es la etiqueta asociada a '<b>gastos generales</b>', etiqueta comodín,
            este dinero se puede emplear en cualquier etiqueta.
            </p>
            <p><b>¿Qué pasa cuando se recibe un 'ingreso con etiqueta'?</b>
            Se comprueba si durante el ciclo semanal el usuario a extradido de su cuenta de '<b>gastos generales</b>' algún importe
            asociado a esa etiqueta, en caso afirmativo se reingresa el importe gastado y
            el sobrante se ingresa en la cuenta que el usuario tiene asociada a esa etiqueta
            </p>
            <p><b>¿Qué pasa cuando se extrae dinero asociándolo a una etiqueta?</b>
            Se comprueba si el usuario tiene disponible sufciente en su cuenta asociada  a esa etiqueta y en caso
            afirmativo se extrae el dinero de esa cuenta, si el disponible en la cuenta es inferior al importe lo que
            falte se obtiene de la cuenta asociada a '<b>gastos generales</b>' (si se dispone de saldo suficiente)
            </p>
            <p><b>¿Qué es un ingreso 'caducable'?</b> Es un ingreso cuyo importe debe gastarse antes de las 24:00 horas
            del domingo de la semana en curso, lo que no se gaste se ingresa en el sistema. Los ingresos <b>caducables</b>
            tienen que estar asociados obligatoriamente a una etiqueta, la unica etiqueta que no permite movimientos
            caducables es la etiqueta WILDTAG (la etiqueta asociada a <b>gastos generales</b>).
            </p>
        </div>
    </div>
</body>
</html>

