/*
 */
/**
 * 
 * <p>
 * Apiscol-Meta Web Service is identified by the <em>/meta</em>
 * root path. It provides functionalities related to sco-LOM-fr <em>metadata</em>.
 * 
 * 
 *  </p><ol>
 *  <li>Validation</li>
 *  <li>Lom, Lomfr retrocompatibility (coming soon)</li>
 *  <li>Indexation, facetted and full-text search</li>
 *  </ol>
 *  <section class="attention">
 *  <h2>Apiscol Meta web service and download links</h2>
 *  <p>
 *  File download requests are not supported by the Apiscol Meta
 *  web service. Please pay attention to the difference between the
 *  <em>/meta</em> path which requests the service to
 *  perform a treatment and the <em>/meta/lom</em> path which
 *  points to the <em>lom</em> directory in order to dowload
 *  metadata files as static contents.
 *  </p>
 *  <p>This is a REST URI that supports the 4 HTTP verbs : <br/>
 *  <code>http://my.apiscol.server/meta/5808af38-9f5a-4e6a-b871-3154bcc1f963</code></p>
 *  <p>This is not a REST URI but a download link : <br/>
 *  <code>http://178.32.219.182/meta/lom/a/2/3/d4b96-dc83-4ad3-88ad-2dbf43068d44.xml</code></p>
 *   </section>
 *  <section class="attention">
 *  <h2>XML and JsonP static delivery</h2>
 *  <p>Static metadata files are available in two formats in <code>lom</code> directory, <code>application/lom+xml</code> and <code>application/javascript</code>. The second one is a workaround for 
 *  browser cross-domain policy. It allows client-side applications to access XML data from outside their domain.
 *  The JsonP format is not really json reencoded : it is only an xml document serialized as raw string, escaped and wrapped in a javascript function call.
 *  <br/>Example : <code>notice("&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt; etc.  );</code>
 *   <br/>It's ready for client-side XSLT transform.</p>
 *  </section>
 *  <section class="attention">
 *  <h2>Optimistic concurrency</h2>
 *  <p>Optimistic concurrency system implemented in apiscol means that the element ATOM "updated" is used to check the freshness of an entry.
 *   Each POST, PUT or DELETE request (but not GET requests) must be accompanied by the last value received for updated in the HTTP header "If-Match".</p>
 *  <p>For exemple : <br/>
 *  The ATOM metadata representation received by the client contains this field :<br/>
 *  <code>&lt;updated&gt;2013-03-17T06:58:49.000+01:00&lt;/updated&gt;</code><br/>
 *  The next POST, PUT OR DELETE REQUEST will have to contain this field :<br/>
 *  <code>If-Match	2013-03-10T08:46:55.000+01:00</code>
 *  </p>
 *  </section>
 *  <section class="attention">
 *  <h2>Apiscol unique writing proxy strategy </h2>
 *  <p>ApiScol Meta is never directly accessed with POST, PUT or DELETE requests.
 *  ApiScol Edit acts like a proxy for all writing operations.</p>
 *  <p>For example, if you intend to modify a metadata entry, you will most of the time not adress directly:
 *  <br/>
 *  <code>PUT http://my.apiscol.server/meta/5808af38-9f5a-4e6a-b871-3154bcc1f963</code><br/>
 *  <p>but :
 *  <br/>
 *  <code>PUT https://my.apiscol.server/edit/meta/5808af38-9f5a-4e6a-b871-3154bcc1f963</code><br/>
 *  </p>
 *  <p>See <a href="../edit">Edit web service API documentation for more information</a></p>
 *  </section>
 *  <section class="attention">
 *  <h2>Static and dynamic facets</h2>
 *   <p>See <a href="../seek">Seek web service API documentation for more information</a></p>
 *   </section>
 *  </section>
 */

package fr.ac_versailles.crdp.apiscol.meta;