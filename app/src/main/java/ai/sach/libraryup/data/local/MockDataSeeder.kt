package ai.sach.libraryup.data.local

import ai.sach.libraryup.BuildConfig
import ai.sach.libraryup.data.local.entity.PaymentHistory
import ai.sach.libraryup.data.local.entity.Property
import ai.sach.libraryup.data.local.entity.Tenant
import ai.sach.libraryup.data.repository.PropertyRepository
import ai.sach.libraryup.data.repository.TenantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

object MockDataSeeder {
    suspend fun seed(properties: PropertyRepository, tenants: TenantRepository): Int = withContext(Dispatchers.IO) {
        if (!BuildConfig.DEBUG) return@withContext 0

        val existing = properties.getAllPropertiesSync()
        if (existing.any { it.id.startsWith("mock-property-") }) return@withContext 0

        val mockProperties = (1..5).map { index ->
            Property(
                id = "mock-property-$index",
                name = "Demo Property $index",
                address = "${index * 10} Market Street, Bengaluru",
                totalUnits = 8 + index,
                floors = 2 + index % 4,
                description = "Seeded property for UI testing",
                type = if (index % 2 == 0) "Apartment" else "House",
                unitRent = 12000 + index * 500,
                occupancyRate = 40 + index * 4
            )
        }
        mockProperties.forEach { properties.insertProperty(it) }

        val today = LocalDate.now()
        val tenantsToSeed = listOf(
            Tenant(
                name = "Paid Tenant",
                status = "Active",
                isPaid = true,
                mobileNumber = "+91 90000 10001",
                dueDate = today.toString(),
                propertyId = "mock-property-1",
                floor = "1",
                amount = "₹12500",
                permanentAddress = "Demo address, Bengaluru"
            ),
            Tenant(
                name = "Overdue Tenant",
                status = "Active",
                isPaid = false,
                mobileNumber = "+91 90000 10002",
                dueDate = today.minusDays(7).toString(),
                propertyId = "mock-property-2",
                floor = "1",
                amount = "₹13000",
                permanentAddress = "Demo address, Bengaluru"
            ),
            Tenant(
                name = "Due Soon Tenant",
                status = "Active",
                isPaid = false,
                mobileNumber = "+91 90000 10003",
                dueDate = today.plusDays(3).toString(),
                propertyId = "mock-property-3",
                floor = "1",
                amount = "₹13500",
                permanentAddress = "Demo address, Bengaluru"
            ),
            Tenant(
                name = "Due Tenant",
                status = "Active",
                isPaid = false,
                mobileNumber = "+91 90000 10004",
                dueDate = today.plusDays(10).toString(),
                propertyId = "mock-property-4",
                floor = "1",
                amount = "₹14000",
                permanentAddress = "Demo address, Bengaluru"
            ),
            Tenant(
                name = "Notice Tenant",
                status = "Notice",
                isPaid = false,
                mobileNumber = "+91 90000 10005",
                dueDate = today.minusDays(2).toString(),
                propertyId = "mock-property-5",
                floor = "1",
                amount = "₹14500",
                permanentAddress = "Demo address, Bengaluru"
            )
        )

        var count = 0
        tenantsToSeed.forEachIndexed { index, tenant ->
            val tenantId = tenants.insertTenant(tenant)
            if (index == 0) {
                tenants.addPaymentHistoryItem(
                    PaymentHistory(
                        tenantId = tenantId,
                        date = today.minusDays(30).toString(),
                        amount = tenant.amount ?: "₹12500",
                        collectedBy = "Demo Owner",
                        remark = "Seeded payment"
                    ),
                    tenantId
                )
            }
            count++
        }
        count
    }
}
