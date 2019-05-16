# -*- mode: python; python-indent: 4 -*-
import ncs
from ncs.application import Service

# ---------------------------------------------
# COMPONENT THREAD THAT WILL BE STARTED BY NCS.
# ---------------------------------------------
class Main(ncs.application.Application):
    def setup(self):
        # The application class sets up logging for us. It is accessible
        # through 'self.log' and is a ncs.log.Log instance.
        self.log.info('Main RUNNING')

        # Service callbacks require a registration for a 'service point',
        # as specified in the corresponding data model.
        #
        self.register_service('storeconnect-servicepoint', ServiceCallbacks)

        # If we registered any callback(s) above, the Application class
        # took care of creating a daemon (related to the service/action point).

        # When this setup method is finished, all registrations are
        # considered done and the application is 'started'.

    def teardown(self):
        # When the application is finished (which would happen if NCS went
        # down, packages were reloaded or some error occurred) this teardown
        # method will be called.

        self.log.info('Main FINISHED')

# ------------------------
# SERVICE CALLBACK EXAMPLE
# ------------------------
class ServiceCallbacks(Service):

    # The create() callback is invoked inside NCS FASTMAP and
    # must always exist.
    @Service.create
    def cb_create(self, tctx, root, publisher, proplist):
        self.log.info('Service create(publisher=', publisher._path, ')')

        vlan_id = self.allocate_vlan(publisher)
        publisher.network.allocated_vlan = vlan_id
        mon  = self.config_e_routers(publisher, vlan_id)
        mon += self.config_i_routers(publisher, vlan_id, root)
        self.config_monitoring(publisher, mon)

        self.log.info('Service creation done')

    def allocate_vlan(self, publisher):
        # Let's make this as simple as possible for now:
        # Just return a hash on the name (1000..2999)
        return 1000 + hash(publisher.name) % 2000

    def config_e_routers(self, publisher, vlan_id):
        mon = []
        for site in publisher.network.site:
            site_interface = self.get_interface(site)
            if bool(site_interface) and bool(site.e_router) and bool(site.address):
                # e-router and address are not marked mandatory in YANG
                # (they could have been => we would not have needed this)
                # Unless both are set, we will simply skip this site
                vars = ncs.template.Variables()
                vars.add('DEVICE', site.e_router)
                vars.add('INTERFACE', site_interface)
                vars.add('ADDRESS', site.address)
                vars.add('MASK_LEN', site.mask_len)
                vars.add('MASK', self.ip_size_to_mask[site.mask_len])
                vars.add('VLAN_ID', vlan_id)
                template = ncs.template.Template(publisher)
                template.apply('e-router-template', vars)
                mon += [("%s-%s-int"%(publisher.name, site.name), site.address, vlan_id)]
        return mon

    def get_interface(self,site):
        if bool(site.junos_interface):
            return site.junos_interface
        if bool(site.ios_ge_interface):
            return "GigabitEthernet"+site.ios_ge_interface
        return site.ietf_interface

    ip_size_to_mask = [
        "0.0.0.0", 
        "128.0.0.0",       "192.0.0.0",       "224.0.0.0",       "240.0.0.0", 
        "248.0.0.0",       "252.0.0.0",       "254.0.0.0",       "255.0.0.0",
        "255.128.0.0",     "255.192.0.0",     "255.224.0.0",     "255.240.0.0",
        "255.248.0.0",     "255.252.0.0",     "255.254.0.0",     "255.255.0.0",
        "255.255.128.0",   "255.255.192.0",   "255.255.224.0",   "255.255.240.0",
        "255.255.248.0",   "255.255.252.0",   "255.255.254.0",   "255.255.255.0",
        "255.255.255.128", "255.255.255.192", "255.255.255.224", "255.255.255.240",
        "255.255.255.248", "255.255.255.252", "255.255.255.254", "255.255.255.255" ]

    def config_i_routers(self, publisher, vlan_id, root):
        mon = []
        for store in root.storeconnect__stores.store:
            connect = False # Assume no connection to this store
            for tag in [x.tag for x in publisher.target_store.tags]:
                if tag in store.tags:
                    # This publisher targets a tag that is 
                    # carried by this store. Let's connect!
                    connect = True
                    break
            if connect:
                self.log.info('connecting store ', store.name, ' to publisher ', publisher.name)
                vars = ncs.template.Variables()
                vars.add('DEVICE', store.network.i_router)
                vars.add('INTERFACE', store.network.interface)
                vars.add('ADDRESS', store.network.address)
                vars.add('VLAN_ID', vlan_id)
                template = ncs.template.Template(publisher)
                template.apply('i-router-template', vars)
                mon += [("%s-%s-ext"%(publisher.name, store.name), store.network.address, vlan_id)]
        for tag in [x.tag for x in publisher.target_store.tags]:                
            publisher.target_store.tags[tag].number_of_stores_with_tag = len(
                [store for store in root.stores.store if tag in store.tags])
        return mon

    def config_monitoring(self, publisher, mon):
        self.log.info('setup monitoring for ', publisher.name, ': ', len(mon), ' legs')
        vars = ncs.template.Variables()
        template = ncs.template.Template(publisher)
        vars.add('DEVICE', 'm0')
        for (mon_name, address, vlan_id) in mon:
            vars.add('MON_NAME', mon_name)
            vars.add('ADDRESS', address)
            vars.add('VLAN_ID', vlan_id)
            template.apply('monitoring-template', vars)
