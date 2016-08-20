
package com.ardic.android.libnsd.nsdhelper;

import com.ardic.android.libnsd.nsdhelper.NetworkServiceDiscovery.NSDService;

public interface ServiceDiscoveryResolverListener {

    public void onNewServiceResolved(NSDService service);

}
