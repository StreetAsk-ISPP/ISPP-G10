import { useState } from 'react';
import {
	Keyboard, Platform, ScrollView, StyleSheet, Text,
	TextInput, TouchableOpacity, TouchableWithoutFeedback,
	View, useWindowDimensions, Linking,
} from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { Ionicons } from '@expo/vector-icons';
import apiClient from '../../../shared/services/http/apiClient';
import { STORAGE_KEYS } from '../../../shared/constants/storageKeys';

export default function BusinessSignupScreen({ navigation, route }) {
	const { email } = route.params;
	const { width } = useWindowDimensions();
	const isNarrow = width < 500;

	const [taxId, setTaxId] = useState('');
	const [companyName, setCompanyName] = useState('');
	const [address, setAddress] = useState('');
	const [website, setWebsite] = useState('');
	const [description, setDescription] = useState('');
	const [error, setError] = useState('');
	const [taxIdError, setTaxIdError] = useState('');
	const [isSubmitting, setIsSubmitting] = useState(false);
	const [focusedField, setFocusedField] = useState(null);

	// Tax ID format validation: 1 letter + 7 digits + 1 alphanumeric character
	const TAX_ID_REGEX = /^[A-Za-z]\d{7}[A-Za-z0-9]$/;

	const handleTaxIdChange = (text) => {
		const upper = text.toUpperCase();
		setTaxId(upper);
		// Provide real-time feedback on tax ID format
		if (upper.length > 0 && !TAX_ID_REGEX.test(upper)) {
			setTaxIdError('Format: 1 letter + 7 digits + 1 character (e.g., A1234567B)');
		} else {
			setTaxIdError('');
		}
	};

	const handleProceedToPayment = async () => {
		setError('');
		setTaxIdError('');
		if (!taxId.trim()) { setError('Tax ID is required.'); return; }
		if (!companyName.trim()) { setError('Company Name is required.'); return; }
		if (!TAX_ID_REGEX.test(taxId)) {
			setError('Tax ID must be: 1 letter + 7 digits + 1 control character (e.g., A1234567B).');
			return;
		}

		try {
			setIsSubmitting(true);
			const normalizedTaxId = taxId.trim().toUpperCase();

			await apiClient.post('/api/v1/auth/signup/business', {
				email,
				taxId: normalizedTaxId,
				companyName: companyName.trim(),
				address: address.trim() || null,
				website: website.trim() || null,
				description: description.trim() || null,
			});

			const checkoutResponse = await apiClient.post('/api/v1/business-subscriptions/stripe/checkout-session', {
				email,
				taxId: normalizedTaxId,
			});

			const checkoutUrl = checkoutResponse?.data?.checkoutUrl;
			if (!checkoutUrl) {
				setError('Stripe checkout session could not be initialized.');
				return;
			}

			if (Platform.OS === 'web' && typeof window !== 'undefined') {
				window.localStorage.setItem(
					STORAGE_KEYS.PENDING_BUSINESS_CHECKOUT,
					JSON.stringify({ email, taxId: normalizedTaxId })
				);
				window.location.assign(checkoutUrl);
				return;
			}

			await Linking.openURL(checkoutUrl);
			setError('Complete the payment in Stripe and return to the app.');
		} catch (err) {
			let msg = err.response?.data?.message || err.response?.data || err.message || 'Registration failed.';
			msg = typeof msg === 'string' ? msg : JSON.stringify(msg);
			if (msg.toLowerCase().includes('invalid tax id') || msg.toLowerCase().includes('tax id format'))
				setError('Tax ID must be: 1 letter + 7 digits + 1 control character.');
			else if (msg.toLowerCase().includes('tax id is already registered') || msg.toLowerCase().includes('taxid is already registered'))
				setError('This Tax ID is already registered.');
			else if (msg.toLowerCase().includes('already completed'))
				setError('This account has already been completed. Please log in.');
			else setError(msg);
		} finally {
			setIsSubmitting(false);
		}
	};

	const renderInput = (label, value, onChangeText, fieldKey, opts = {}) => (
		<View style={{ marginTop: 14 }}>
			<Text style={styles.label}>{label}</Text>
			<View style={[
				styles.inputWrapper,
				focusedField === fieldKey && styles.inputFocused,
				opts.multiline && { height: 90, alignItems: 'flex-start', paddingTop: 12 },
			]}>
				{opts.icon && <Ionicons name={opts.icon} size={18} color="#1f2937" style={{ marginRight: 10 }} />}
				<TextInput
					value={value}
					onChangeText={onChangeText}
					autoCapitalize={opts.autoCapitalize || 'sentences'}
					placeholder={opts.placeholder || ''}
					placeholderTextColor="#c0c5ce"
					multiline={opts.multiline}
					style={[styles.input, opts.multiline && { textAlignVertical: 'top' }]}
					onFocus={() => setFocusedField(fieldKey)}
					onBlur={() => setFocusedField(null)}
				/>
			</View>
		</View>
	);

	const benefits = [
		'Sponsored events & live chats',
		'Event promotion & management tools',
		'Increased visibility',
		'Unlimited events',
		'Full event customization',
	];

	const content = (
		<View style={styles.screen}>
			<ScrollView
				contentContainerStyle={styles.scrollContent}
				showsVerticalScrollIndicator={false}
				keyboardShouldPersistTaps="handled"
			>
				<View style={[styles.card, isNarrow && { marginHorizontal: 16 }]}>
					{/* Back */}
					<TouchableOpacity style={styles.backBtn} onPress={() => navigation.goBack()} activeOpacity={0.7}>
						<Ionicons name="chevron-back" size={24} color="#667eea" />
					</TouchableOpacity>

					{/* Header */}
					<View style={styles.headerCircle}>
						<Ionicons name="star" size={28} color="#fff" />
					</View>
					<Text style={styles.title}>Business Account</Text>
					<Text style={styles.subtitle}>Complete your business profile</Text>

					{/* Fields */}
					{renderInput('Tax ID *', taxId, handleTaxIdChange, 'tax', { icon: 'pricetag-outline', autoCapitalize: 'characters', placeholder: 'A1234567B' })}
					{taxIdError ? <Text style={styles.warningText}>{taxIdError}</Text> : (taxId && TAX_ID_REGEX.test(taxId) ? <Text style={styles.successText}>✓ Valid format</Text> : null)}
					{renderInput('Company Name *', companyName, setCompanyName, 'company', { icon: 'business-outline', placeholder: 'Acme Inc.' })}
					{renderInput('Address', address, setAddress, 'addr', { icon: 'location-outline', placeholder: '123 Main St.' })}
					{renderInput('Website', website, setWebsite, 'web', { icon: 'globe-outline', autoCapitalize: 'none', placeholder: 'https://...' })}
					{renderInput('Description', description, setDescription, 'desc', { icon: 'document-text-outline', multiline: true, placeholder: 'Tell us about your company...' })}

					{error ? <Text style={styles.errorText}>{error}</Text> : null}

					{/* Benefits */}
					<View style={styles.benefitsCard}>
						<Text style={styles.benefitsTitle}>What you get</Text>
						{benefits.map((b, i) => (
							<View key={i} style={styles.benefitRow}>
								<Ionicons name="checkmark-circle" size={18} color="#667eea" />
								<Text style={styles.benefitText}>{b}</Text>
							</View>
						))}
					</View>

					{/* Proceed */}
					<TouchableOpacity
						onPress={handleProceedToPayment}
						disabled={isSubmitting}
						activeOpacity={0.85}
					>
						<LinearGradient
							colors={['#667eea', '#764ba2']}
							start={{ x: 0, y: 0 }}
							end={{ x: 1, y: 1 }}
							style={styles.proceedBtn}
						>
							<Ionicons name="card-outline" size={20} color="#fff" />
							<Text style={styles.proceedBtnText}>
								{isSubmitting ? 'Processing...' : 'Proceed to Payment'}
							</Text>
						</LinearGradient>
					</TouchableOpacity>
				</View>
			</ScrollView>
		</View>
	);

	if (Platform.OS === 'web') return content;
	return (
		<TouchableWithoutFeedback onPress={Keyboard.dismiss} accessible={false}>
			{content}
		</TouchableWithoutFeedback>
	);
}

const styles = StyleSheet.create({
	screen: {
		flex: 1,
		backgroundColor: '#f3f4f6',
	},
	scrollContent: {
		flexGrow: 1,
		justifyContent: 'center',
		alignItems: 'center',
		padding: 24,
	},
	card: {
		width: '100%',
		maxWidth: 460,
		backgroundColor: '#fff',
		borderRadius: 28,
		padding: 28,
		shadowColor: '#000',
		shadowOffset: { width: 0, height: 8 },
		shadowOpacity: 0.06,
		shadowRadius: 24,
		elevation: 6,
	},
	backBtn: {
		width: 40,
		height: 40,
		borderRadius: 12,
		backgroundColor: '#f3f4f6',
		alignItems: 'center',
		justifyContent: 'center',
		marginBottom: 12,
	},
	headerCircle: {
		width: 64,
		height: 64,
		borderRadius: 32,
		backgroundColor: '#f59e0b',
		alignSelf: 'center',
		alignItems: 'center',
		justifyContent: 'center',
		marginBottom: 16,
		shadowColor: '#f59e0b',
		shadowOffset: { width: 0, height: 6 },
		shadowOpacity: 0.25,
		shadowRadius: 16,
		elevation: 8,
	},
	title: {
		fontSize: 24,
		fontWeight: '800',
		color: '#1f2937',
		textAlign: 'center',
	},
	subtitle: {
		fontSize: 14,
		color: '#9ca3af',
		textAlign: 'center',
		marginTop: 4,
	},
	label: {
		fontSize: 13,
		fontWeight: '600',
		color: '#374151',
		marginBottom: 6,
	},
	inputWrapper: {
		flexDirection: 'row',
		alignItems: 'center',
		backgroundColor: '#f9fafb',
		borderWidth: 1.5,
		borderColor: '#e5e7eb',
		borderRadius: 14,
		paddingHorizontal: 14,
		height: 48,
	},
	inputFocused: {
		borderColor: '#667eea',
		backgroundColor: '#fff',
	},
	input: {
		flex: 1,
		fontSize: 15,
		color: '#1f2937',
		height: '100%',
		outlineStyle: 'none',
	},
	errorText: {
		color: '#ef4444',
		fontSize: 13,
		textAlign: 'center',
		marginTop: 14,
	},
	warningText: {
		color: '#f59e0b',
		fontSize: 12,
		marginTop: 6,
		marginBottom: 4,
	},
	successText: {
		color: '#10b981',
		fontSize: 12,
		marginTop: 6,
		marginBottom: 4,
	},
	benefitsCard: {
		backgroundColor: '#f9fafb',
		borderRadius: 16,
		padding: 16,
		marginTop: 20,
		marginBottom: 20,
		borderWidth: 1,
		borderColor: '#e5e7eb',
	},
	benefitsTitle: {
		fontSize: 15,
		fontWeight: '700',
		color: '#1f2937',
		marginBottom: 10,
	},
	benefitRow: {
		flexDirection: 'row',
		alignItems: 'center',
		gap: 10,
		marginBottom: 8,
	},
	benefitText: {
		fontSize: 13,
		color: '#374151',
		flex: 1,
	},
	proceedBtn: {
		borderRadius: 14,
		paddingVertical: 16,
		flexDirection: 'row',
		alignItems: 'center',
		justifyContent: 'center',
		gap: 10,
		shadowColor: '#667eea',
		shadowOffset: { width: 0, height: 6 },
		shadowOpacity: 0.25,
		shadowRadius: 16,
		elevation: 5,
	},
	proceedBtnText: {
		color: '#fff',
		fontWeight: '700',
		fontSize: 16,
	},
});
