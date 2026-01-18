package app.marlboroadvance.mpvex.di

import app.marlboroadvance.mpvex.domain.anime4k.Anime4KManager
import app.marlboroadvance.mpvex.domain.translation.MLKitTranslationService
import app.marlboroadvance.mpvex.domain.translation.TranslationService
import app.marlboroadvance.mpvex.domain.repository.TranslationRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext

val domainModule = module {
    single { Anime4KManager(androidContext()) }
    
    single<TranslationService> { MLKitTranslationService() }
    singleOf(::TranslationRepository)
}
